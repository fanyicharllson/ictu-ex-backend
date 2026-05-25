package com.fanyiadrien.auth.internal

import com.fanyiadrien.auth.AuthResult
import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.auth.AuthUser
import com.fanyiadrien.auth.internal.persistence.UserEntity
import com.fanyiadrien.auth.internal.persistence.UserRepository
import com.fanyiadrien.auth.internal.persistence.UserType
import com.fanyiadrien.shared.events.VerificationCodeGeneratedEvent
import com.fanyiadrien.shared.events.UserVerifiedEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fanyiadrien.shared.redis.TokenBlacklistService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

internal object ErrorMessages {
    const val INVALID_EMAIL = "Email must be a valid ICT University email address"
    const val EMAIL_ALREADY_REGISTERED = "Email already registered"
    const val STUDENT_ID_ALREADY_REGISTERED = "Student ID already registered"
    const val USER_ID_NOT_GENERATED = "User ID not generated"
    const val VERIFICATION_CODE_NOT_GENERATED = "Verification code not generated"
    const val INVALID_TOKEN = "Invalid token"
    const val USER_NOT_FOUND = "User not found"
    const val ACCOUNT_ALREADY_VERIFIED = "Account already verified"
    const val INVALID_CREDENTIALS = "Invalid email or password"
    const val USER_ID_IS_NULL = "User ID is null"
    const val INVALID_VERIFICATION_CODE = "Invalid or expired verification code"
}

@Service
internal class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: EventPublisher,
    private val tokenBlacklistService: TokenBlacklistService
) : AuthService {

    override fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): AuthResult {

        if (!isValidIctuEmail(email))
            throw IllegalArgumentException(ErrorMessages.INVALID_EMAIL)

        if (userRepository.existsByEmail(email))
            throw IllegalArgumentException(ErrorMessages.EMAIL_ALREADY_REGISTERED)

        if (userRepository.existsByStudentId(studentId))
            throw IllegalArgumentException(ErrorMessages.STUDENT_ID_ALREADY_REGISTERED)

        val user = UserEntity(
            email = email,
            displayName = displayName,
            studentId = studentId,
            userType = UserType.STUDENT,
            passwordHash = passwordEncoder.encode(password)
        )

        user.generateVerificationCode()
        val savedUser = userRepository.save(user)

        eventPublisher.publish(
            topic = KafkaTopics.VERIFICATION_CODE_GENERATED,
            event = VerificationCodeGeneratedEvent(
                userId = savedUser.id ?: throw IllegalStateException(ErrorMessages.USER_ID_NOT_GENERATED),
                email = savedUser.email,
                displayName = savedUser.displayName,
                code = savedUser.verificationCode
                    ?: user.verificationCode
                    ?: throw IllegalStateException(ErrorMessages.VERIFICATION_CODE_NOT_GENERATED)
            )
        )

        val token = jwtService.generateToken(savedUser.id, savedUser.email)
        return AuthResult(token = token, user = savedUser.toAuthUser(), message = "Registered successfully! Verification code sent to your email.")
    }

    override fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException(ErrorMessages.INVALID_CREDENTIALS)

        if (!passwordEncoder.matches(password, user.passwordHash))
            throw IllegalArgumentException(ErrorMessages.INVALID_CREDENTIALS)

        val token = jwtService.generateToken(user.id ?: throw IllegalStateException(ErrorMessages.USER_ID_IS_NULL), user.email)
        return AuthResult(token = token, user = user.toAuthUser(), message = "Login successfully!")
    }

    override fun validateToken(token: String): AuthUser? {
        if (!jwtService.isTokenValid(token)) return null
        val userId = jwtService.extractUserId(token)
        return userRepository.findById(userId).orElse(null)?.toAuthUser()
    }

    override fun updateUserType(token: String, userType: String): AuthUser {
        if (!jwtService.isTokenValid(token))
            throw IllegalArgumentException(ErrorMessages.INVALID_TOKEN)

        val userId = jwtService.extractUserId(token)
        val existingUser = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException(ErrorMessages.USER_NOT_FOUND)
        }

        val parsedType = parseUserType(userType)
        val updatedUser = userRepository.save(
            UserEntity(
                id = existingUser.id,
                email = existingUser.email,
                displayName = existingUser.displayName,
                studentId = existingUser.studentId,
                userType = parsedType,
                passwordHash = existingUser.passwordHash,
                profileImageUrl = existingUser.profileImageUrl,
                verificationCode = existingUser.verificationCode,
                verificationCodeExpiresAt = existingUser.verificationCodeExpiresAt,
                isVerified = existingUser.isVerified,
                createdAt = existingUser.createdAt,
                updatedAt = Instant.now()
            )
        )

        return updatedUser.toAuthUser()
    }

    override fun getUserById(userId: UUID): AuthUser? {
        return userRepository.findById(userId).orElse(null)?.toAuthUser()
    }

    override fun verifyCode(email: String, code: String): Boolean {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException(ErrorMessages.USER_NOT_FOUND)

        if (user.isVerified) return true

        if (user.isCodeValid(code)) {
            user.isVerified = true
            user.verificationCode = null
            user.verificationCodeExpiresAt = null
            user.updatedAt = Instant.now()
            val savedUser = userRepository.save(user)

            eventPublisher.publish(
                topic = KafkaTopics.USER_VERIFIED,
                event = UserVerifiedEvent(
                    userId = savedUser.id ?: throw IllegalStateException(ErrorMessages.USER_ID_NOT_GENERATED),
                    email = savedUser.email,
                    displayName = savedUser.displayName
                )
            )
            return true
        }

        throw IllegalArgumentException(ErrorMessages.INVALID_VERIFICATION_CODE)
    }

    override fun resendVerificationCode(email: String) {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException(ErrorMessages.USER_NOT_FOUND)

        if (user.isVerified) throw IllegalArgumentException(ErrorMessages.ACCOUNT_ALREADY_VERIFIED)

        user.generateVerificationCode()
        user.updatedAt = Instant.now()
        val savedUser = userRepository.save(user)

        eventPublisher.publish(
            topic = KafkaTopics.VERIFICATION_CODE_GENERATED,
            event = VerificationCodeGeneratedEvent(
                userId = savedUser.id ?: throw IllegalStateException(ErrorMessages.USER_ID_NOT_GENERATED),
                email = savedUser.email,
                displayName = savedUser.displayName,
                code = savedUser.verificationCode
                    ?: user.verificationCode
                    ?: throw IllegalStateException(ErrorMessages.VERIFICATION_CODE_NOT_GENERATED)
            )
        )
    }

    override fun logout(token: String) {
        val remainingExpiry = jwtService.getRemainingExpiry(token)
        if (remainingExpiry > 0) {
            val jti = jwtService.extractJti(token) ?: return
            tokenBlacklistService.blacklist(jti, remainingExpiry)
        }
    }

    private fun UserEntity.toAuthUser() = AuthUser(
        id = id ?: throw IllegalStateException(ErrorMessages.USER_ID_IS_NULL),
        email = email,
        displayName = displayName,
        studentId = studentId,
        userType = userType.name
    )

    private fun isValidIctuEmail(email: String): Boolean {
        return email.trim().endsWith("@ictuniversity.edu.cm")
    }

    private fun parseUserType(userType: String): UserType {
        return try {
            UserType.valueOf(userType.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("User type must be STUDENT, BUYER or SELLER")
        }
    }
}
