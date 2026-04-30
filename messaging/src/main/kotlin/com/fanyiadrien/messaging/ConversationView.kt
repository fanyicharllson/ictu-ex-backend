package com.fanyiadrien.messaging

import java.time.Instant
import java.util.UUID

/**
 * Public read model exposed by the messaging module.
 * Other modules depend on this — never on internal classes.
 */
data class ConversationView(
    val id: UUID,
    val participantA: UUID,
    val participantB: UUID,
    val listingId: UUID?,
    val createdAt: Instant
)
