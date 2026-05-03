package com.fanyiadrien.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
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
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResult> {
        val result = authService.login(request.email, request.password)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/validate")
    fun validate(@RequestHeader("Authorization") token: String): ResponseEntity<*> {
        val bearerToken = token.removePrefix("Bearer ")
        val user = authService.validateToken(bearerToken)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build<Void>()
        }
    }

    /* Method to update the user type(either to a seller or buyer)*/
    @PatchMapping("/user-type")
    fun updateUserType(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: UpdateUserTypeRequest
    ): ResponseEntity<AuthUser> {
        val bearerToken = token.removePrefix("Bearer ")
        val updatedUser = authService.updateUserType(bearerToken, request.userType)
        return ResponseEntity.ok(updatedUser)
    }

    /* Resend verification code token */
    @PostMapping("/resend-token")
    fun resendToken(@RequestBody request: ResendTokenRequest): ResponseEntity<MessageResponse> {
        authService.resendVerificationCode(request.email)
        return ResponseEntity.ok(MessageResponse(message = "Verification code resent successfully!"))
    }

    /* Verify the 6-digit code sent by email */
    @PostMapping("/verify-code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<MessageResponse> {
        authService.verifyCode(request.email, request.code)
        return ResponseEntity.ok(MessageResponse(message = "Account verified successfully!"))
    }




}

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val studentId: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdateUserTypeRequest(
    val userType: String
)

data class ResendTokenRequest(
    val email: String
)

data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class MessageResponse(
    val message: String
)

