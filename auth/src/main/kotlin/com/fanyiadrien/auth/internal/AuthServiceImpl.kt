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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
internal class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: EventPublisher
) : AuthService {

    override fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): AuthResult {

        if (!isValidIctuEmail(email))
            throw IllegalArgumentException("Email must be a valid ICT University email address")

        if (userRepository.existsByEmail(email))
            throw IllegalArgumentException("Email already registered")

        if (userRepository.existsByStudentId(studentId))
            throw IllegalArgumentException("Student ID already registered")

        val user = UserEntity(
            email = email,
            displayName = displayName,
            studentId = studentId,
            userType = UserType.STUDENT,
            passwordHash = passwordEncoder.encode(password)
        )
        
        user.generateVerificationCode()
        val savedUser = userRepository.save(user)

        // Publish event for verification email
        eventPublisher.publish(
            topic = KafkaTopics.VERIFICATION_CODE_GENERATED,
            event = VerificationCodeGeneratedEvent(
                userId = savedUser.id ?: throw IllegalStateException("User ID not generated"),
                email = savedUser.email,
                displayName = savedUser.displayName,
                code = savedUser.verificationCode
                    ?: user.verificationCode
                    ?: throw IllegalStateException("Verification code not generated")
            )
        )

        val token = jwtService.generateToken(savedUser.id, savedUser.email)
        return AuthResult(token = token, user = savedUser.toAuthUser(), message = "Registered successfully! Verification code sent to your email.")
    }

    override fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(password, user.passwordHash))
            throw IllegalArgumentException("Invalid email or password")

        val token = jwtService.generateToken(user.id ?: throw IllegalStateException("User ID is null"), user.email)
        return AuthResult(token = token, user = user.toAuthUser(), message = "Login successfully!")
    }

    override fun validateToken(token: String): AuthUser? {
        if (!jwtService.isTokenValid(token)) return null
        val userId = jwtService.extractUserId(token)
        return userRepository.findById(userId).orElse(null)?.toAuthUser()
    }

    override fun updateUserType(token: String, userType: String): AuthUser {
        if (!jwtService.isTokenValid(token)) {
            throw IllegalArgumentException("Invalid token")
        }

        val userId = jwtService.extractUserId(token)
        val existingUser = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
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
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("User not found")
        
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
                    userId = savedUser.id ?: throw IllegalStateException("User ID not generated"),
                    email = savedUser.email,
                    displayName = savedUser.displayName
                )
            )
            return true
        }
        
        throw IllegalArgumentException("Invalid or expired verification code")
    }

    override fun resendVerificationCode(email: String) {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("User not found")
        
        if (user.isVerified) throw IllegalArgumentException("Account already verified")
        
        user.generateVerificationCode()
        user.updatedAt = Instant.now()
        val savedUser = userRepository.save(user)

        eventPublisher.publish(
            topic = KafkaTopics.VERIFICATION_CODE_GENERATED,
            event = VerificationCodeGeneratedEvent(
                userId = savedUser.id ?: throw IllegalStateException("User ID not generated"),
                email = savedUser.email,
                displayName = savedUser.displayName,
                code = savedUser.verificationCode
                    ?: user.verificationCode
                    ?: throw IllegalStateException("Verification code not generated")
            )
        )
    }

    private fun UserEntity.toAuthUser() = AuthUser(
        id = id ?: throw IllegalStateException("User ID is null"),
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
