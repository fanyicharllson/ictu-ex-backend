package com.fanyiadrien.ictuexbackend.config

import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.shared.redis.TokenBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@Component
class JwtAuthenticationFilter(
    private val authService: AuthService,
    private val tokenBlacklistService: TokenBlacklistService
) : OncePerRequestFilter() {

    private val publicPaths = listOf(
        "/api/auth/register",
        "/api/auth/login",
        "/actuator",
        "/swagger-ui",
        "/swagger-ui.html",
        "/api-docs",
        "/v3/api-docs"
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

        if (token == null || !authService.isTokenValid(token)) {
            sendUnauthorized(response, "Invalid or missing token")
            return
        }

        val jti = authService.extractTokenJti(token)
        if (jti == null || tokenBlacklistService.isBlacklisted(jti)) {
            sendUnauthorized(response, "Token has been invalidated — please login again")
            return
        }

        try {
            val userId = authService.extractTokenUserId(token)
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
        response.writer.write("""{"message":"$message","status":401,"error":"Unauthorized"}""")
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ")
    }
}
