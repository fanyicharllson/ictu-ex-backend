package com.fanyiadrien.ictuexbackend.config

import com.fanyiadrien.shared.redis.TokenBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.fanyiadrien.auth.internal.JwtService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val tokenBlacklistService: TokenBlacklistService
) : OncePerRequestFilter() {

    private val publicPaths = listOf(
        "/api/auth/register",
        "/api/auth/login",
        "/actuator"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (publicPaths.any { path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val token = extractToken(request)

        if (token == null || !jwtService.isTokenValid(token)) {
            sendUnauthorized(response, "Invalid or missing token")
            return
        }

        // Check Redis blacklist using the token identifier (jti)
        val jti = jwtService.extractJti(token)
        if (jti == null || tokenBlacklistService.isBlacklisted(jti)) {
            sendUnauthorized(response, "Token has been invalidated — please login again")
            return
        }

        // Populate SecurityContext with the authenticated user id
        try {
            val userId = jwtService.extractUserId(token)
            val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        } catch (_: Exception) {
            sendUnauthorized(response, "Invalid token")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun sendUnauthorized(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write(
            """{"message":"$message","status":401,"error":"Unauthorized"}"""
        )
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ")
    }
}