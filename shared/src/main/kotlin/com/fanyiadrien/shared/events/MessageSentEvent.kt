package com.fanyiadrien.shared.events

import java.time.Instant
import java.util.UUID

data class MessageSentEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val senderName: String,
    val receiverId: UUID,
    val receiverName: String,
    val receiverEmail: String,
    val content: String,
    val occurredAt: Instant = Instant.now()
)
