package com.fanyiadrien.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fanyiadrien.shared.common.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(SyncController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class SyncControllerTest {

    @SpringBootApplication
    class TestApplication

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var syncService: SyncService

    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val userId = UUID.randomUUID()

    // ==================== POST /api/sync/pull ====================

    @Test
    fun `pullSync returns 200 with listings and messages`() {
        val response = PullSyncResponse(
            listings = listOf(mapOf("id" to "abc", "title" to "Laptop")),
            messages = listOf(mapOf("id" to "xyz", "content" to "Hello")),
            syncedAt = Instant.now()
        )
        whenever(syncService.pullSync(any(), any())).thenReturn(response)

        mockMvc.post("/api/sync/pull") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                PullSyncRequest(userId = userId, lastSyncedAt = Instant.now().minusSeconds(3600))
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.listings[0].title") { value("Laptop") }
            jsonPath("$.messages[0].content") { value("Hello") }
        }
    }

    @Test
    fun `pullSync returns 200 with empty lists when no data`() {
        whenever(syncService.pullSync(any(), any())).thenReturn(PullSyncResponse())

        mockMvc.post("/api/sync/pull") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                PullSyncRequest(userId = userId, lastSyncedAt = Instant.now().minusSeconds(3600))
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.listings") { isEmpty() }
            jsonPath("$.messages") { isEmpty() }
        }
    }

    // ==================== POST /api/sync/heartbeat ====================

    @Test
    fun `heartbeat returns 200 with online status`() {
        whenever(syncService.heartbeat(any())).thenReturn(
            HeartbeatResponse(status = "online", serverTime = Instant.now())
        )

        mockMvc.post("/api/sync/heartbeat") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(HeartbeatRequest(userId = userId))
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("online") }
        }
    }

    @Test
    fun `heartbeat returns 400 for malformed request`() {
        mockMvc.post("/api/sync/heartbeat") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"invalid": "body"}"""
        }.andExpect {
            // malformed JSON with missing required field causes bad request via GlobalExceptionHandler
            status { isBadRequest() }
        }
    }

    // ==================== GET /api/sync/status/{userId} ====================

    @Test
    fun `getSyncStatus returns 200 with status`() {
        val lastSynced = Instant.now().minusSeconds(60)
        whenever(syncService.getSyncStatus(userId)).thenReturn(
            SyncStatus(userId = userId, lastSyncedAt = lastSynced, pendingItems = 3L)
        )

        mockMvc.get("/api/sync/status/$userId").andExpect {
            status { isOk() }
            jsonPath("$.userId") { value(userId.toString()) }
            jsonPath("$.pendingItems") { value(3) }
        }
    }

    @Test
    fun `getSyncStatus returns 200 with null lastSyncedAt when no history`() {
        whenever(syncService.getSyncStatus(userId)).thenReturn(
            SyncStatus(userId = userId, lastSyncedAt = null, pendingItems = 0L)
        )

        mockMvc.get("/api/sync/status/$userId").andExpect {
            status { isOk() }
            jsonPath("$.pendingItems") { value(0) }
        }
    }
}
