package com.fanyiadrien.messaging.internal.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "conversations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["participant_a", "participant_b", "listing_id"])]
)
class ConversationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "participant_a", nullable = false)
    val participantA: UUID,

    @Column(name = "participant_b", nullable = false)
    val participantB: UUID,

    @Column(name = "listing_id")
    val listingId: UUID? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
