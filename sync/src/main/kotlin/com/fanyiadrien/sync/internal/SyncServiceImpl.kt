package com.fanyiadrien.sync.internal

import com.fanyiadrien.sync.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
internal class SyncServiceImpl(
    private val syncRepository: SyncRepository,
    private val objectMapper: ObjectMapper // For deserializing payload
) : SyncService {

    override fun pullSync(userId: UUID, lastSyncedAt: Instant): PullSyncResponse {
        val records = syncRepository.findByUserIdAndCreatedAtAfter(userId, lastSyncedAt)

        val listings = mutableListOf<Map<String, Any>>()
        val messages = mutableListOf<Map<String, Any>>()
        // Add other entity types as needed

        records.forEach { record ->
            val payloadMap = record.payload?.let {
                objectMapper.readValue(it, Map::class.java) as Map<String, Any>
            } ?: emptyMap()

            when (record.entityType) {
                "LISTING" -> listings.add(payloadMap + ("action" to record.action))
                "MESSAGE" -> messages.add(payloadMap + ("action" to record.action))
                // Handle other entity types
            }
        }

        return PullSyncResponse(
            listings = listings,
            messages = messages,
            syncedAt = Instant.now()
        )
    }

    override fun heartbeat(userId: UUID): HeartbeatResponse {
        // In a real scenario, you might update a user's last seen timestamp here
        return HeartbeatResponse(serverTime = Instant.now())
    }

    override fun getSyncStatus(userId: UUID): SyncStatus {
        val lastSyncedAt = syncRepository.findTopByUserIdOrderByCreatedAtDesc(userId)?.createdAt
        val pendingItems = if (lastSyncedAt != null) {
            syncRepository.countByUserIdAndCreatedAtAfter(userId, lastSyncedAt)
        } else {
            syncRepository.countByUserId(userId)
        }
        return SyncStatus(userId = userId, lastSyncedAt = lastSyncedAt, pendingItems = pendingItems)
    }
}
