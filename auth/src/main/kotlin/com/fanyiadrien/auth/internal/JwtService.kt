package com.fanyiadrien.auth.internal

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: UUID, email: String): String {
        return Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun extractUserId(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        val subject = claims.subject ?: throw IllegalArgumentException("Token does not contain a user ID")
        return UUID.fromString(subject)
    }

    fun extractJti(token: String): String? {
        return try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            claims.id
        } catch (_: Exception) {
            null
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getRemainingExpiry(token: String): Long {
        return try {
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            val expiry = claims.expiration.time
            val remaining = (expiry - System.currentTimeMillis()) / 1000
            if (remaining > 0) remaining else 0
        } catch (_: Exception) {
            0
        }
    }
}