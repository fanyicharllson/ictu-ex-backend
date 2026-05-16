package com.fanyiadrien.sync

import java.time.Instant
import java.util.UUID

interface SyncService {
    fun pullSync(userId: UUID, lastSyncedAt: Instant): PullSyncResponse
    fun heartbeat(userId: UUID): HeartbeatResponse
    fun getSyncStatus(userId: UUID): SyncStatus
}

data class PullSyncResponse(
    val listings: List<Map<String, Any>> = emptyList(),
    val messages: List<Map<String, Any>> = emptyList(),
    val syncedAt: Instant = Instant.now()
)

data class HeartbeatResponse(
    val status: String = "online",
    val serverTime: Instant = Instant.now()
)

data class SyncStatus(
    val userId: UUID,
    val lastSyncedAt: Instant?,
    val pendingItems: Long
)
