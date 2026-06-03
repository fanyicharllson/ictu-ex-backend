package com.fanyiadrien.auth.internal

import com.fanyiadrien.auth.internal.persistence.UserEntity
import com.fanyiadrien.auth.internal.persistence.UserRepository
import com.fanyiadrien.auth.internal.persistence.UserType
import com.fanyiadrien.auth.internal.ErrorMessages
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.redis.TokenBlacklistService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceImplTest {

    // Real implementations for unit testing
    private val passwordEncoder = BCryptPasswordEncoder()

    // Mocks — fake versions of dependencies
    private val userRepository: UserRepository = mock()
    private val jwtService: JwtService = mock()
    private val eventPublisher: EventPublisher = mock()
    private val tokenBlacklistService: TokenBlacklistService = mock()


    private lateinit var authService: AuthServiceImpl

    @BeforeEach
    fun setup() {
        authService = AuthServiceImpl(
            userRepository = userRepository,
            jwtService = jwtService,
            passwordEncoder = passwordEncoder,
            eventPublisher = eventPublisher,
            tokenBlacklistService = tokenBlacklistService
        )
    }

    // ==================== REGISTER TESTS ====================

    @Test
    fun `register succeeds with valid ICTU email`() {
        // Arrange
        val email = "john.doe@ictuniversity.edu.cm"
        val savedUser = buildUserEntity(email = email)

        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(userRepository.existsByStudentId("ICT001")).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(jwtService.generateToken(any(), any())).thenReturn("mock.jwt.token")

        // Act
        val result = authService.register(
            email = email,
            password = "password123",
            displayName = "John Doe",
            studentId = "ICT001"
        )

        // Assert
        assertNotNull(result)
        assertEquals("mock.jwt.token", result.token)
        assertEquals(email, result.user.email)
        verify(eventPublisher).publish(any(), any()) // Kafka event was published
    }

    @Test
    fun `register throws exception for non-ICTU email`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByStudentId(any())).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(buildUserEntity())
        whenever(jwtService.generateToken(any(), any())).thenReturn("token")

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(
                email = "john@gmail.com",
                password = "password123",
                displayName = "John Doe",
                studentId = "ICT001"
            )
        }
        assertEquals(
            ErrorMessages.INVALID_EMAIL,
            exception.message
        )
        verify(userRepository, never()).save(any()) // DB never called
    }

    @Test
    fun `register throws exception for duplicate email`() {
        val email = "john@ictuniversity.edu.cm"
        whenever(userRepository.existsByEmail(email)).thenReturn(true)

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(
                email = email,
                password = "password123",
                displayName = "John Doe",
                studentId = "ICT001"
            )
        }
        assertEquals(ErrorMessages.EMAIL_ALREADY_REGISTERED, exception.message)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register throws exception for duplicate student ID`() {
        val email = "john@ictuniversity.edu.cm"
        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(userRepository.existsByStudentId("ICT001")).thenReturn(true)

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(
                email = email,
                password = "password123",
                displayName = "John Doe",
                studentId = "ICT001"
            )
        }
        assertEquals(ErrorMessages.STUDENT_ID_ALREADY_REGISTERED, exception.message)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register publishes Kafka event after successful registration`() {
        val email = "john@ictuniversity.edu.cm"
        val savedUser = buildUserEntity(email = email)

        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(userRepository.existsByStudentId("ICT001")).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(jwtService.generateToken(any(), any())).thenReturn("token")

        authService.register(email, "password123", "John Doe", "ICT001")

        // Verify Kafka event was published exactly once
        verify(eventPublisher, times(1)).publish(any(), any())
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun `login succeeds with correct credentials`() {
        val email = "john@ictuniversity.edu.cm"
        val password = "password123"
        val user = buildUserEntity(
            email = email,
            passwordHash = passwordEncoder.encode(password)
        )

        whenever(userRepository.findByEmail(email)).thenReturn(user)
        whenever(jwtService.generateToken(any(), any())).thenReturn("mock.jwt.token")

        val result = authService.login(email, password)

        assertNotNull(result)
        assertEquals("mock.jwt.token", result.token)
        assertEquals(email, result.user.email)
    }

    @Test
    fun `login throws exception for non-existent email`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            authService.login("nobody@ictuniversity.edu.cm", "password123")
        }
        assertEquals(ErrorMessages.INVALID_CREDENTIALS, exception.message)
    }

    @Test
    fun `login throws exception for wrong password`() {
        val email = "john@ictuniversity.edu.cm"
        val user = buildUserEntity(
            email = email,
            passwordHash = passwordEncoder.encode("correctpassword")
        )
        whenever(userRepository.findByEmail(email)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            authService.login(email, "wrongpassword")
        }
        assertEquals(ErrorMessages.INVALID_CREDENTIALS, exception.message)
    }

    // ==================== VALIDATE TOKEN TESTS ====================

    @Test
    fun `validateToken returns AuthUser for valid token`() {
        val userId = UUID.randomUUID()
        val user = buildUserEntity(id = userId)

        whenever(jwtService.isTokenValid("valid.token")).thenReturn(true)
        whenever(jwtService.extractUserId("valid.token")).thenReturn(userId)
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        val result = authService.validateToken("valid.token")

        assertNotNull(result)
        assertEquals(userId, result?.id)
    }

    @Test
    fun `validateToken returns null for invalid token`() {
        whenever(jwtService.isTokenValid("bad.token")).thenReturn(false)

        val result = authService.validateToken("bad.token")

        assertNull(result)
        verify(userRepository, never()).findById(any())
    }

    // ==================== UPDATE USER TYPE TESTS ====================

    @Test
    fun `updateUserType succeeds for valid token and type`() {
        val userId = UUID.randomUUID()
        val existingUser = buildUserEntity(id = userId)
        val updatedUser = UserEntity(
            id = userId,
            email = existingUser.email,
            displayName = existingUser.displayName,
            studentId = existingUser.studentId,
            userType = UserType.BUYER,
            passwordHash = existingUser.passwordHash,
            profileImageUrl = existingUser.profileImageUrl,
            createdAt = existingUser.createdAt
        )

        whenever(jwtService.isTokenValid("valid.token")).thenReturn(true)
        whenever(jwtService.extractUserId("valid.token")).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any())).thenReturn(updatedUser)

        val result = authService.updateUserType("valid.token", "buyer")

        assertEquals("BUYER", result.userType)
    }

    @Test
    fun `updateUserType throws exception for invalid token`() {
        whenever(jwtService.isTokenValid("bad.token")).thenReturn(false)

        val exception = assertThrows<IllegalArgumentException> {
            authService.updateUserType("bad.token", "BUYER")
        }

        assertEquals(ErrorMessages.INVALID_TOKEN, exception.message)
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `updateUserType throws exception for unsupported type`() {
        val userId = UUID.randomUUID()
        val existingUser = buildUserEntity(id = userId)

        whenever(jwtService.isTokenValid("valid.token")).thenReturn(true)
        whenever(jwtService.extractUserId("valid.token")).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))

        val exception = assertThrows<IllegalArgumentException> {
            authService.updateUserType("valid.token", "ADMIN")
        }

        assertEquals("User type must be STUDENT, BUYER or SELLER", exception.message)
        verify(userRepository, never()).save(any())
    }

    // ==================== VERIFY CODE TESTS ====================

    @Test
    fun `verifyCode succeeds and publishes verified event`() {
        val user = buildUserEntity(email = "verified@ictuniversity.edu.cm")
        user.generateVerificationCode()

        whenever(userRepository.findByEmail(user.email)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as UserEntity }

        val result = authService.verifyCode(user.email, user.verificationCode!!)

        assertTrue(result)
        verify(eventPublisher).publish(any(), any())
    }

    @Test
    fun `verifyCode returns true immediately when already verified`() {
        val user = buildUserEntity()
        user.isVerified = true
        whenever(userRepository.findByEmail(user.email)).thenReturn(user)

        val result = authService.verifyCode(user.email, "any-code")

        assertTrue(result)
        verify(userRepository, never()).save(any())
        verify(eventPublisher, never()).publish(any(), any())
    }

    @Test
    fun `verifyCode throws for invalid code`() {
        val user = buildUserEntity()
        user.generateVerificationCode()
        whenever(userRepository.findByEmail(user.email)).thenReturn(user)

        val ex = assertThrows<IllegalArgumentException> {
            authService.verifyCode(user.email, "WRONG-CODE")
        }
        assertEquals(ErrorMessages.INVALID_VERIFICATION_CODE, ex.message)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `verifyCode throws when user not found`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)

        val ex = assertThrows<IllegalArgumentException> {
            authService.verifyCode("nobody@ictuniversity.edu.cm", "code")
        }
        assertEquals(ErrorMessages.USER_NOT_FOUND, ex.message)
    }

    // ==================== RESEND VERIFICATION CODE TESTS ====================

    @Test
    fun `resendVerificationCode sends code when user is unverified`() {
        val user = buildUserEntity()
        user.isVerified = false
        whenever(userRepository.findByEmail(user.email)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as UserEntity }

        authService.resendVerificationCode(user.email)

        verify(userRepository).save(any())
        verify(eventPublisher).publish(any(), any())
    }

    @Test
    fun `resendVerificationCode throws when user is already verified`() {
        val user = buildUserEntity()
        user.isVerified = true
        whenever(userRepository.findByEmail(user.email)).thenReturn(user)

        val ex = assertThrows<IllegalArgumentException> {
            authService.resendVerificationCode(user.email)
        }
        assertEquals(ErrorMessages.ACCOUNT_ALREADY_VERIFIED, ex.message)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `resendVerificationCode throws when user not found`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)

        val ex = assertThrows<IllegalArgumentException> {
            authService.resendVerificationCode("nobody@ictuniversity.edu.cm")
        }
        assertEquals(ErrorMessages.USER_NOT_FOUND, ex.message)
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    fun `logout blacklists token jti when token has remaining expiry`() {
        whenever(jwtService.getRemainingExpiry("valid.token")).thenReturn(3600L)
        whenever(jwtService.extractJti("valid.token")).thenReturn("jti-abc")

        authService.logout("valid.token")

        verify(tokenBlacklistService).blacklist("jti-abc", 3600L)
    }

    @Test
    fun `logout does nothing when token is already expired`() {
        whenever(jwtService.getRemainingExpiry("expired.token")).thenReturn(0L)

        authService.logout("expired.token")

        verify(tokenBlacklistService, never()).blacklist(any(), any())
    }

    @Test
    fun `logout does nothing when jti is null`() {
        whenever(jwtService.getRemainingExpiry("token")).thenReturn(3600L)
        whenever(jwtService.extractJti("token")).thenReturn(null)

        authService.logout("token")

        verify(tokenBlacklistService, never()).blacklist(any(), any())
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    fun `getUserById returns AuthUser when found`() {
        val userId = UUID.randomUUID()
        val user = buildUserEntity(id = userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = authService.getUserById(userId)

        assertNotNull(result)
        assertEquals(userId, result?.id)
    }

    @Test
    fun `getUserById returns null when not found`() {
        val userId = UUID.randomUUID()
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        val result = authService.getUserById(userId)

        assertNull(result)
    }

    // ==================== TOKEN DELEGATION TESTS (new AuthService methods) ====================

    @Test
    fun `isTokenValid delegates to jwtService`() {
        whenever(jwtService.isTokenValid("token")).thenReturn(true)
        assertTrue(authService.isTokenValid("token"))

        whenever(jwtService.isTokenValid("bad")).thenReturn(false)
        assertFalse(authService.isTokenValid("bad"))
    }

    @Test
    fun `extractTokenJti delegates to jwtService`() {
        whenever(jwtService.extractJti("token")).thenReturn("jti-123")
        assertEquals("jti-123", authService.extractTokenJti("token"))
    }

    @Test
    fun `extractTokenUserId delegates to jwtService`() {
        val userId = UUID.randomUUID()
        whenever(jwtService.extractUserId("token")).thenReturn(userId)
        assertEquals(userId, authService.extractTokenUserId("token"))
    }

    // ==================== HELPER ====================

    private fun buildUserEntity(
        id: UUID = UUID.randomUUID(),
        email: String = "test@ictuniversity.edu.cm",
        passwordHash: String = "hashed",
        studentId: String = "ICT001"
    ) = UserEntity(
        id = id,
        email = email,
        displayName = "Test User",
        studentId = studentId,
        userType = UserType.STUDENT,
        passwordHash = passwordHash
    )
}