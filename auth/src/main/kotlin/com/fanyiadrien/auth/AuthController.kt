package com.fanyiadrien.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Student authentication endpoints")
class AuthController(private val authService: AuthService) {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new ICTU student",
        description = "Only @ictuniversity.edu.cm emails accepted"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Registration successful — returns JWT token"),
            ApiResponse(responseCode = "400", description = "Invalid email, duplicate email or duplicate student ID")
        ]
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResult> {
        val result = authService.register(
            email = request.email,
            password = request.password,
            displayName = request.displayName,
            studentId = request.studentId
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/login")
    @Operation(summary = "Student login")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful — returns JWT token"),
            ApiResponse(responseCode = "400", description = "Invalid credentials")
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResult> {
        val result = authService.login(request.email, request.password)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token valid — returns user info"),
            ApiResponse(responseCode = "401", description = "Token invalid or expired")
        ]
    )
    fun validate(@RequestHeader("Authorization") token: String): ResponseEntity<*> {
        val user = authService.validateToken(token.removePrefix(BEARER_PREFIX))
        return if (user != null) ResponseEntity.ok(user)
        else ResponseEntity.notFound().build<Unit>()
    }

    @PatchMapping("/user-type")
    @Operation(summary = "Update user role type")
    @SecurityRequirement(name = "bearerAuth")
    fun updateUserType(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: UpdateUserTypeRequest
    ): ResponseEntity<AuthUser> {
        val updatedUser = authService.updateUserType(token.removePrefix(BEARER_PREFIX), request.userType)
        return ResponseEntity.ok(updatedUser)
    }

    @PostMapping("/resend-token")
    @Operation(summary = "Resend account verification code")
    fun resendToken(@RequestBody request: ResendTokenRequest): ResponseEntity<MessageResponse> {
        authService.resendVerificationCode(request.email)
        return ResponseEntity.ok(MessageResponse(message = "Verification code resent successfully!"))
    }

    @PostMapping("/verify-code")
    @Operation(summary = "Verify 6-digit email code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<MessageResponse> {
        authService.verifyCode(request.email, request.code)
        return ResponseEntity.ok(MessageResponse(message = "Account verified successfully!"))
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — invalidates JWT immediately via Redis blacklist")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Logged out successfully"),
            ApiResponse(responseCode = "401", description = "Token already invalid")
        ]
    )
    fun logout(@RequestHeader("Authorization") token: String): ResponseEntity<Map<String, String>> {
        authService.logout(token.removePrefix(BEARER_PREFIX))
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

data class RegisterRequest(
    @field:Schema(example = "john.doe@ictuniversity.edu.cm")
    val email: String,
    @field:Schema(example = "SecurePass123")
    val password: String,
    @field:Schema(example = "John Doe")
    val displayName: String,
    @field:Schema(example = "ICT2024001")
    val studentId: String
)

data class LoginRequest(
    @field:Schema(example = "john.doe@ictuniversity.edu.cm")
    val email: String,
    @field:Schema(example = "SecurePass123")
    val password: String
)

data class UpdateUserTypeRequest(val userType: String)

data class ResendTokenRequest(val email: String)

data class VerifyCodeRequest(val email: String, val code: String)

data class MessageResponse(val message: String)
