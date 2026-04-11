package com.fanyiadrien.shared.events

import java.time.Instant
import java.util.UUID

data class UserRegisteredEvent(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val studentId: String,
    val occurredAt: Instant = Instant.now()
)