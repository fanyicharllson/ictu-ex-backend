package com.fanyiadrien.sync.internal

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

internal interface SyncRepository : JpaRepository<SyncRecord, UUID> {
    fun findByUserIdAndCreatedAtAfter(userId: UUID, createdAt: Instant): List<SyncRecord>
    fun countByUserIdAndCreatedAtAfter(userId: UUID, createdAt: Instant): Long
    fun countByUserId(userId: UUID): Long
    fun findTopByUserIdOrderByCreatedAtDesc(userId: UUID): SyncRecord?
}
