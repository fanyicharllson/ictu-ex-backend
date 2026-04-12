package com.fanyiadrien.auth.internal

import com.fanyiadrien.auth.AuthResult
import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.auth.AuthUser
import com.fanyiadrien.auth.internal.persistence.UserEntity
import com.fanyiadrien.auth.internal.persistence.UserRepository
import com.fanyiadrien.auth.internal.persistence.UserType
import com.fanyiadrien.shared.events.UserRegisteredEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

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

        //Check if email is valid ICTU email
        if (!isValidIctuEmail(email))
            throw IllegalArgumentException("Email must be a valid ICT University email address")

        //Check if student already exit
        if (userRepository.existsByEmail(email))
            throw IllegalArgumentException("Email already registered")

        //Check if ID(matrcule already exist)
        if (userRepository.existsByStudentId(studentId))
            throw IllegalArgumentException("Student ID already registered")

        val user = userRepository.save(
            UserEntity(
                email = email,
                displayName = displayName,
                studentId = studentId,
                userType = UserType.STUDENT,
                passwordHash = passwordEncoder.encode(password)
            )
        )

        // Publish Kafka event — notification module will consume this
        eventPublisher.publish(
            topic = KafkaTopics.USER_REGISTERED,
            event = UserRegisteredEvent(
                userId = user.id!!,
                email = user.email,
                displayName = user.displayName,
                studentId = user.studentId
            )
        )

        val token = jwtService.generateToken(user.id!!, user.email)
        return AuthResult(token = token, user = user.toAuthUser(), message = "Registered successfully!")
    }

    override fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(password, user.passwordHash))
            throw IllegalArgumentException("Invalid email or password")

        val token = jwtService.generateToken(user.id!!, user.email)
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
                createdAt = existingUser.createdAt,
                updatedAt = Instant.now()
            )
        )

        return updatedUser.toAuthUser()
    }

    private fun UserEntity.toAuthUser() = AuthUser(
        id = id!!,
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