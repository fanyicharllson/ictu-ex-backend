package com.fanyiadrien.auth

import java.util.UUID

/**
 * Public read model exposed by the auth module.
 * Other modules depend on this — never on internal classes.
 */
data class AuthUser(
    val id: UUID,
    val email: String,
    val displayName: String,
    val studentId: String,
    val userType: String
)