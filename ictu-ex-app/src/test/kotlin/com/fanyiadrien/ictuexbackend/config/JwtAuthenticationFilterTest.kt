package com.fanyiadrien.ictuexbackend.config

import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.shared.redis.TokenBlacklistService
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class JwtAuthenticationFilterTest {

    private val authService = mock(AuthService::class.java)
    private val tokenBlacklistService = mock(TokenBlacklistService::class.java)
    private val filter = JwtAuthenticationFilter(authService, tokenBlacklistService)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `public endpoint bypasses authentication checks`() {
        val request = MockHttpServletRequest("GET", "/api/auth/login")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertTrue(chainCalled)
        verify(authService, never()).isTokenValid(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `missing authorization header returns unauthorized`() {
        val request = MockHttpServletRequest("GET", "/api/listings")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains("Invalid or missing token"))
    }

    @Test
    fun `malformed bearer header returns unauthorized`() {
        val request = MockHttpServletRequest("GET", "/api/listings")
        request.addHeader("Authorization", "Token abc")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains("Invalid or missing token"))
    }

    @Test
    fun `blacklisted token returns unauthorized`() {
        val token = "jwt-token"
        `when`(authService.isTokenValid(token)).thenReturn(true)
        `when`(authService.extractTokenJti(token)).thenReturn("jti-123")
        `when`(tokenBlacklistService.isBlacklisted("jti-123")).thenReturn(true)

        val request = MockHttpServletRequest("GET", "/api/listings")
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains("Token has been invalidated"))
    }

    @Test
    fun `valid token sets authentication and continues filter chain`() {
        val token = "jwt-token"
        val userId = UUID.randomUUID()
        `when`(authService.isTokenValid(token)).thenReturn(true)
        `when`(authService.extractTokenJti(token)).thenReturn("jti-123")
        `when`(tokenBlacklistService.isBlacklisted("jti-123")).thenReturn(false)
        `when`(authService.extractTokenUserId(token)).thenReturn(userId)

        val request = MockHttpServletRequest("GET", "/api/listings")
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertTrue(chainCalled)
        assertEquals(200, response.status)
        assertEquals(
            UsernamePasswordAuthenticationToken::class.java,
            SecurityContextHolder.getContext().authentication.javaClass
        )
        assertEquals(userId, SecurityContextHolder.getContext().authentication.principal)
    }

    @Test
    fun `token parsing failure returns unauthorized`() {
        val token = "jwt-token"
        `when`(authService.isTokenValid(token)).thenReturn(true)
        `when`(authService.extractTokenJti(token)).thenReturn("jti-123")
        `when`(tokenBlacklistService.isBlacklisted("jti-123")).thenReturn(false)
        `when`(authService.extractTokenUserId(token)).thenThrow(RuntimeException("bad token"))

        val request = MockHttpServletRequest("GET", "/api/listings")
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains("Invalid token"))
    }
}

