package com.fanyiadrien.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fanyiadrien.shared.common.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class AuthControllerTest {

    // Local boot config lets @WebMvcTest bootstrap in this module without depending on ictu-ex-app package layout.
    @SpringBootApplication
    class TestApplication

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var authService: AuthService

    private val mockUser = AuthUser(
        id = UUID.randomUUID(),
        email = "test@ictuniversity.edu.cm",
        displayName = "Test Student",
        studentId = "ICT001",
        userType = "STUDENT"
    )

    private val mockResult = AuthResult(
        token = "mock.jwt.token",
        user = mockUser,
        message = "Registered successfully!"
    )

    @Test
    fun `register returns 200 with valid request`() {
        whenever(authService.register(any(), any(), any(), any()))
            .thenReturn(mockResult)

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(
                    email = "test@ictuniversity.edu.cm",
                    password = "password123",
                    displayName = "Test Student",
                    studentId = "ICT001"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `register returns 400 for duplicate email`() {
        whenever(authService.register(any(), any(), any(), any()))
            .thenThrow(IllegalArgumentException("Email already registered"))

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(
                    email = "test@ictuniversity.edu.cm",
                    password = "password123",
                    displayName = "Test Student",
                    studentId = "ICT001"
                )
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `login returns 200 with valid credentials`() {
        whenever(authService.login(any(), any())).thenReturn(mockResult)

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(
                    email = "test@ictuniversity.edu.cm",
                    password = "password123"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `login returns 400 for wrong password`() {
        whenever(authService.login(any(), any()))
            .thenThrow(IllegalArgumentException("Invalid email or password"))

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(
                    email = "test@ictuniversity.edu.cm",
                    password = "wrongpassword"
                )
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `validate returns 200 for valid token`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)

        mockMvc.get("/api/auth/validate") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isOk() }
        }
    }
}
