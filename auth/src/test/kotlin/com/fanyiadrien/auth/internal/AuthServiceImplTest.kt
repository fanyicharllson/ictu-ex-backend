package com.fanyiadrien.auth.internal

import com.fanyiadrien.auth.internal.persistence.UserEntity
import com.fanyiadrien.auth.internal.persistence.UserRepository
import com.fanyiadrien.auth.internal.persistence.UserType
import com.fanyiadrien.shared.kafka.EventPublisher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

class AuthServiceImplTest {

    // Real implementations for unit testing
    private val passwordEncoder = BCryptPasswordEncoder()

    // Mocks — fake versions of dependencies
    private val userRepository: UserRepository = mock()
    private val jwtService: JwtService = mock()
    private val eventPublisher: EventPublisher = mock()

    private lateinit var authService: AuthServiceImpl

    @BeforeEach
    fun setup() {
        authService = AuthServiceImpl(
            userRepository = userRepository,
            jwtService = jwtService,
            passwordEncoder = passwordEncoder,
            eventPublisher = eventPublisher
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
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(
                email = "john@gmail.com",
                password = "password123",
                displayName = "John Doe",
                studentId = "ICT001"
            )
        }
        assertEquals(
            "Email must be a valid ICT University email address",
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
        assertEquals("Email already registered", exception.message)
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
        assertEquals("Student ID already registered", exception.message)
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
        assertEquals("Invalid email or password", exception.message)
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
        assertEquals("Invalid email or password", exception.message)
    }

    // ==================== VALIDATE TOKEN TESTS ====================

    @Test
    fun `validateToken returns AuthUser for valid token`() {
        val userId = UUID.randomUUID()
        val user = buildUserEntity(id = userId)

        whenever(jwtService.isTokenValid("valid.token")).thenReturn(true)
        whenever(jwtService.extractUserId("valid.token")).thenReturn(userId)
        whenever(userRepository.findById(userId))
            .thenReturn(java.util.Optional.of(user))

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