package com.fanyiadrien.sync.internal

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sync_records")
internal class SyncRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "entity_type", nullable = false)
    val entityType: String, // LISTING / MESSAGE / USER

    @Column(name = "entity_id", nullable = false)
    val entityId: UUID,

    @Column(name = "action", nullable = false)
    val action: String, // CREATED / UPDATED / DELETED

    @Column(name = "payload", columnDefinition = "TEXT")
    val payload: String?, // JSON of the changed entity

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
