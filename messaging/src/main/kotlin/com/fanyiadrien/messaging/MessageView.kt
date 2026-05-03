package com.fanyiadrien.messaging

import java.time.Instant
import java.util.UUID

/**
 * Public read model exposed by the messaging module.
 * Other modules depend on this — never on internal classes.
 */
data class MessageView(
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val content: String,
    val sentAt: Instant
)
