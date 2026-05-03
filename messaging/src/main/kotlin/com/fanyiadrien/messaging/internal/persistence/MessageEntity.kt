package com.fanyiadrien.messaging.internal.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messages")
class MessageEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "conversation_id", nullable = false)
    val conversationId: UUID,

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "sent_at", nullable = false, updatable = false)
    val sentAt: Instant = Instant.now()
)
