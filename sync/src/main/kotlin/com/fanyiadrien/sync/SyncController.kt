package com.fanyiadrien.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sync", description = "Offline sync endpoints")
@SecurityRequirement(name = "bearerAuth") // Assuming all sync endpoints require authentication
class SyncController(private val syncService: SyncService) {

    @PostMapping("/pull")
    @Operation(summary = "Pull data for offline synchronization",
        description = "Returns all data that changed since the lastSyncedAt timestamp for the given user.")
    fun pullSync(@RequestBody request: PullSyncRequest): ResponseEntity<PullSyncResponse> {
        val response = syncService.pullSync(request.userId, request.lastSyncedAt)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Send heartbeat to confirm online status",
        description = "Android calls every 30s to confirm online status and get server time.")
    fun heartbeat(@RequestBody request: HeartbeatRequest): ResponseEntity<HeartbeatResponse> {
        val response = syncService.heartbeat(request.userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Check sync status for a user",
        description = "Returns the last synced timestamp and number of pending items for a user.")
    fun getSyncStatus(@PathVariable userId: UUID): ResponseEntity<SyncStatus> {
        val status = syncService.getSyncStatus(userId)
        return ResponseEntity.ok(status)
    }
}

data class PullSyncRequest(
    val userId: UUID,
    val lastSyncedAt: Instant
)

data class HeartbeatRequest(
    val userId: UUID
)
