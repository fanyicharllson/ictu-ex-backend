package com.fanyiadrien.sync.internal

import com.fanyiadrien.sync.HeartbeatResponse
import com.fanyiadrien.sync.SyncStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class SyncServiceImplTest {

    private val syncRepository: SyncRepository = mock()
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private lateinit var syncService: SyncServiceImpl

    @BeforeEach
    fun setUp() {
        syncService = SyncServiceImpl(syncRepository, objectMapper)
    }

    @Test
    fun `pullSync returns empty when no records exist`() {
        val userId = UUID.randomUUID()
        val lastSyncedAt = Instant.now().minusSeconds(3600)
        whenever(syncRepository.findByUserIdAndCreatedAtAfter(userId, lastSyncedAt)).thenReturn(emptyList())

        val response = syncService.pullSync(userId, lastSyncedAt)

        assertTrue(response.listings.isEmpty())
        assertTrue(response.messages.isEmpty())
        assertNotNull(response.syncedAt)
        verify(syncRepository).findByUserIdAndCreatedAtAfter(userId, lastSyncedAt)
    }

    @Test
    fun `pullSync returns records after lastSyncedAt`() {
        val userId = UUID.randomUUID()
        val lastSyncedAt = Instant.now().minusSeconds(3600)
        val listingId = UUID.randomUUID()
        val messageId = UUID.randomUUID()

        val listingPayload = mapOf("id" to listingId.toString(), "title" to "Test Listing")
        val messagePayload = mapOf("id" to messageId.toString(), "content" to "Hello")

        val records = listOf(
            SyncRecord(
                userId = userId,
                entityType = "LISTING",
                entityId = listingId,
                action = "CREATED",
                payload = objectMapper.writeValueAsString(listingPayload),
                createdAt = lastSyncedAt.plusSeconds(100)
            ),
            SyncRecord(
                userId = userId,
                entityType = "MESSAGE",
                entityId = messageId,
                action = "CREATED",
                payload = objectMapper.writeValueAsString(messagePayload),
                createdAt = lastSyncedAt.plusSeconds(200)
            )
        )
        whenever(syncRepository.findByUserIdAndCreatedAtAfter(userId, lastSyncedAt)).thenReturn(records)

        val response = syncService.pullSync(userId, lastSyncedAt)

        assertEquals(1, response.listings.size)
        assertEquals(1, response.messages.size)
        assertEquals(listingId.toString(), response.listings[0]["id"])
        assertEquals("CREATED", response.listings[0]["action"])
        assertEquals(messageId.toString(), response.messages[0]["id"])
        assertEquals("CREATED", response.messages[0]["action"])
        assertNotNull(response.syncedAt)
        verify(syncRepository).findByUserIdAndCreatedAtAfter(userId, lastSyncedAt)
    }

    @Test
    fun `pullSync filters by userId correctly`() {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val lastSyncedAt = Instant.now().minusSeconds(3600)

        val recordForUser1 = SyncRecord(
            userId = userId1,
            entityType = "LISTING",
            entityId = UUID.randomUUID(),
            action = "CREATED",
            payload = "{}",
            createdAt = lastSyncedAt.plusSeconds(100)
        )
        val recordForUser2 = SyncRecord(
            userId = userId2,
            entityType = "MESSAGE",
            entityId = UUID.randomUUID(),
            action = "CREATED",
            payload = "{}",
            createdAt = lastSyncedAt.plusSeconds(200)
        )

        whenever(syncRepository.findByUserIdAndCreatedAtAfter(userId1, lastSyncedAt)).thenReturn(listOf(recordForUser1))
        whenever(syncRepository.findByUserIdAndCreatedAtAfter(userId2, lastSyncedAt)).thenReturn(listOf(recordForUser2))

        val response1 = syncService.pullSync(userId1, lastSyncedAt)
        assertEquals(1, response1.listings.size)
        assertTrue(response1.messages.isEmpty())

        val response2 = syncService.pullSync(userId2, lastSyncedAt)
        assertTrue(response2.listings.isEmpty())
        assertEquals(1, response2.messages.size)

        verify(syncRepository, times(2)).findByUserIdAndCreatedAtAfter(any(), eq(lastSyncedAt))
    }

    @Test
    fun `heartbeat returns correct server time`() {
        val userId = UUID.randomUUID()
        val response = syncService.heartbeat(userId)

        assertEquals("online", response.status)
        assertNotNull(response.serverTime)
        // Verify that no repository methods were called for heartbeat
        verifyNoInteractions(syncRepository)
    }

    @Test
    fun `getSyncStatus returns status when records exist`() {
        val userId = UUID.randomUUID()
        val latestRecordTime = Instant.now().minusSeconds(60)
        val mockLatestRecord = SyncRecord(
            userId = userId,
            entityType = "LISTING",
            entityId = UUID.randomUUID(),
            action = "CREATED",
            payload = "{}",
            createdAt = latestRecordTime
        )

        whenever(syncRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(mockLatestRecord)
        whenever(syncRepository.countByUserIdAndCreatedAtAfter(userId, latestRecordTime)).thenReturn(5L)

        val status = syncService.getSyncStatus(userId)

        assertEquals(userId, status.userId)
        assertEquals(latestRecordTime, status.lastSyncedAt)
        assertEquals(5L, status.pendingItems)
        verify(syncRepository).findTopByUserIdOrderByCreatedAtDesc(userId)
        verify(syncRepository).countByUserIdAndCreatedAtAfter(userId, latestRecordTime)
    }

    @Test
    fun `getSyncStatus returns status when no records exist`() {
        val userId = UUID.randomUUID()

        whenever(syncRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(null)
        whenever(syncRepository.countByUserId(userId)).thenReturn(0L)

        val status = syncService.getSyncStatus(userId)

        assertEquals(userId, status.userId)
        assertNull(status.lastSyncedAt)
        assertEquals(0L, status.pendingItems)
        verify(syncRepository).findTopByUserIdOrderByCreatedAtDesc(userId)
        verify(syncRepository).countByUserId(userId)
        verify(syncRepository, never()).countByUserIdAndCreatedAtAfter(any(), any())
    }
}
