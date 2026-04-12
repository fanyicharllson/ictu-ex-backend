package com.fanyiadrien.auth.internal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    // Test secret must be at least 32 characters for HMAC-SHA
    private val secret = "test-secret-key-for-junit-testing-ictu-ex-2026"
    private val expiration = 86400000L // 24 hours

    @BeforeEach
    fun setup() {
        jwtService = JwtService(secret, expiration)
    }

    @Test
    fun `generateToken returns non-null token`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "test@ictuniversity.edu.cm")
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `generateToken returns valid JWT format`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "test@ictuniversity.edu.cm")
        // JWT has 3 parts separated by dots
        assertEquals(3, token.split(".").size)
    }

    @Test
    fun `extractUserId returns correct UUID`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "test@ictuniversity.edu.cm")
        val extracted = jwtService.extractUserId(token)
        assertEquals(userId, extracted)
    }

    @Test
    fun `isTokenValid returns true for valid token`() {
        val token = jwtService.generateToken(
            UUID.randomUUID(),
            "test@ictuniversity.edu.cm"
        )
        assertTrue(jwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid returns false for tampered token`() {
        val token = jwtService.generateToken(
            UUID.randomUUID(),
            "test@ictuniversity.edu.cm"
        )
        val tampered = token.dropLast(5) + "xxxxx"
        assertFalse(jwtService.isTokenValid(tampered))
    }

    @Test
    fun `isTokenValid returns false for expired token`() {
        // Create jwt service with -1ms expiration (already expired)
        val expiredJwtService = JwtService(secret, -1L)
        val token = expiredJwtService.generateToken(
            UUID.randomUUID(),
            "test@ictuniversity.edu.cm"
        )
        assertFalse(expiredJwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid returns false for empty string`() {
        assertFalse(jwtService.isTokenValid(""))
    }

    @Test
    fun `isTokenValid returns false for random string`() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"))
    }
}