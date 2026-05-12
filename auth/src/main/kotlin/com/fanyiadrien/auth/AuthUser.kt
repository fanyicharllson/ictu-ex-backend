package com.fanyiadrien.auth

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Public read model exposed by the auth module.
 * Other modules depend on this — never on internal classes.
 */
data class AuthUser(
    @field:Schema(description = "User ID")
    val id: UUID,
    @field:Schema(description = "Student email")
    val email: String,
    @field:Schema(description = "Display name")
    val displayName: String,
    @field:Schema(description = "ICTU student ID")
    val studentId: String,
    @field:Schema(description = "Marketplace role")
    val userType: String
)