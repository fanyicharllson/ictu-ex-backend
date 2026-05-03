package com.fanyiadrien.shared.events

import java.time.Instant
import java.util.UUID

data class UserVerifiedEvent(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val verifiedAt: Instant = Instant.now()
)

