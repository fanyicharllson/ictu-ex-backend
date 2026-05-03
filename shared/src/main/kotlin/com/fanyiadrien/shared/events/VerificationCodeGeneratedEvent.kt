package com.fanyiadrien.shared.events

import java.util.UUID

data class VerificationCodeGeneratedEvent(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val code: String
)
