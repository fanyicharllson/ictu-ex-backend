package com.fanyiadrien.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.auth.AuthUser
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

@WebMvcTest(MessagingController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class MessagingControllerTest {

    @SpringBootApplication
    class TestApplication

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var messagingService: MessagingService

    @MockitoBean
    lateinit var authService: AuthService

    private val userAId = UUID.randomUUID()
    private val userBId = UUID.randomUUID()
    private val conversationId = UUID.randomUUID()
    private val listingId = UUID.randomUUID()

    private val mockUser = AuthUser(
        id = userAId,
        email = "student@ictuniversity.edu.cm",
        displayName = "Student A",
        studentId = "ICT001",
        userType = "BUYER"
    )

    private val mockConversation = ConversationView(
        id = conversationId,
        participantA = userAId,
        participantB = userBId,
        listingId = null,
        createdAt = Instant.now()
    )

    private val mockMessage = MessageView(
        id = UUID.randomUUID(),
        conversationId = conversationId,
        senderId = userAId,
        content = "Hello!",
        sentAt = Instant.now()
    )

    // ==================== POST /conversations ====================

//    @Test
//    fun `startConversation returns 200 with valid token`() {
//        whenever(authService.validateToken(any())).thenReturn(mockUser)
//        whenever(messagingService.getOrCreateConversation(any(), any(), any())).thenReturn(mockConversation)
//
//        mockMvc.post("/api/messaging/conversations") {
//            header("Authorization", "Bearer mock.jwt.token")
//            accept = MediaType.APPLICATION_JSON
//            contentType = MediaType.APPLICATION_JSON
//            content = objectMapper.writeValueAsString(StartConversationRequest(otherUserId = userBId))
//        }.andExpect {
//            status { isOk() }
//            jsonPath("$.id") { value(conversationId.toString()) }
//            jsonPath("$.participantA") { value(userAId.toString()) }
//            jsonPath("$.participantB") { value(userBId.toString()) }
//
//        }
//    }

    @Test
    fun `startConversation returns 200 with listingId when provided`() {
        val conversationWithListing = mockConversation.copy(listingId = listingId)
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getOrCreateConversation(any(), any(), any())).thenReturn(conversationWithListing)

        mockMvc.post("/api/messaging/conversations") {
            header("Authorization", "Bearer mock.jwt.token")
            accept = MediaType.APPLICATION_JSON
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                StartConversationRequest(otherUserId = userBId, listingId = listingId)
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.listingId") { value(listingId.toString()) }
        }
    }

    @Test
    fun `startConversation returns 400 for invalid token`() {
        whenever(authService.validateToken(any())).thenReturn(null)

        mockMvc.post("/api/messaging/conversations") {
            header("Authorization", "Bearer bad.token")
            accept = MediaType.APPLICATION_JSON
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(StartConversationRequest(otherUserId = userBId))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Invalid or expired token") }
        }
    }

//    @Test
//    fun `startConversation returns 400 when messaging service throws`() {
//        whenever(authService.validateToken(any())).thenReturn(mockUser)
//        whenever(messagingService.getOrCreateConversation(any(), any(), any()))
//            .thenThrow(IllegalArgumentException("Cannot start a conversation with yourself"))
//
//        mockMvc.post("/api/messaging/conversations") {
//            header("Authorization", "Bearer mock.jwt.token")
//            contentType = MediaType.APPLICATION_JSON
//            content = objectMapper.writeValueAsString(StartConversationRequest(otherUserId = userAId))
//        }.andExpect {
//            status { isBadRequest() }
//            jsonPath("$.message") { value("Cannot start a conversation with yourself") }
//        }
//    }

    // ==================== POST /conversations/{id}/messages ====================

    @Test
    fun `sendMessage returns 200 with message body`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.sendMessage(any(), any(), any())).thenReturn(mockMessage)

        mockMvc.post("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SendMessageRequest(content = "Hello!"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.conversationId") { value(conversationId.toString()) }
            jsonPath("$.senderId") { value(userAId.toString()) }
            jsonPath("$.content") { value("Hello!") }
        }
    }

    @Test
    fun `sendMessage returns 400 for invalid token`() {
        whenever(authService.validateToken(any())).thenReturn(null)

        mockMvc.post("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer bad.token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SendMessageRequest(content = "Hello!"))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Invalid or expired token") }
        }
    }

    @Test
    fun `sendMessage returns 400 when sender is not a participant`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.sendMessage(any(), any(), any()))
            .thenThrow(IllegalArgumentException("You are not a participant of this conversation"))

        mockMvc.post("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SendMessageRequest(content = "Hello!"))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("You are not a participant of this conversation") }
        }
    }

    @Test
    fun `sendMessage returns 400 for blank content`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.sendMessage(any(), any(), any()))
            .thenThrow(IllegalArgumentException("Message content cannot be empty"))

        mockMvc.post("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SendMessageRequest(content = "   "))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Message content cannot be empty") }
        }
    }

    @Test
    fun `sendMessage returns 400 when conversation not found`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.sendMessage(any(), any(), any()))
            .thenThrow(IllegalArgumentException("Conversation not found"))

        mockMvc.post("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SendMessageRequest(content = "Hello!"))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Conversation not found") }
        }
    }

    // ==================== GET /conversations/{id}/messages ====================

    @Test
    fun `getMessages returns 200 with message list`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getMessages(any(), any())).thenReturn(listOf(mockMessage))

        mockMvc.get("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].content") { value("Hello!") }
            jsonPath("$[0].senderId") { value(userAId.toString()) }
        }
    }

    @Test
    fun `getMessages returns 200 with empty list when no messages`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getConversationsForUser(any())).thenReturn(emptyList())

        mockMvc.get("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `getMessages returns 400 for invalid token`() {
        whenever(authService.validateToken(any())).thenReturn(null)

        mockMvc.get("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer bad.token")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Invalid or expired token") }
        }
    }

    @Test
    fun `getMessages returns 400 when requester is not a participant`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getMessages(any(), any()))
            .thenThrow(IllegalArgumentException("You are not a participant of this conversation"))

        mockMvc.get("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("You are not a participant of this conversation") }
        }
    }

    @Test
    fun `getMessages returns 400 when conversation not found`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getMessages(any(), any()))
            .thenThrow(IllegalArgumentException("Conversation not found"))

        mockMvc.get("/api/messaging/conversations/$conversationId/messages") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Conversation not found") }
        }
    }

    // ==================== GET /conversations ====================

    @Test
    fun `getMyConversations returns 200 with conversation list`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getConversationsForUser(any())).thenReturn(listOf(mockConversation))

        mockMvc.get("/api/messaging/conversations") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(conversationId.toString()) }
            jsonPath("$[0].participantA") { value(userAId.toString()) }
            jsonPath("$[0].participantB") { value(userBId.toString()) }
        }
    }

    @Test
    fun `getMyConversations returns 200 with empty list when user has no conversations`() {
        whenever(authService.validateToken(any())).thenReturn(mockUser)
        whenever(messagingService.getConversationsForUser(any())).thenReturn(emptyList())

        mockMvc.get("/api/messaging/conversations") {
            header("Authorization", "Bearer mock.jwt.token")
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `getMyConversations returns 400 for invalid token`() {
        whenever(authService.validateToken(any())).thenReturn(null)

        mockMvc.get("/api/messaging/conversations") {
            header("Authorization", "Bearer bad.token")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Invalid or expired token") }
        }
    }
}
