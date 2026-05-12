package com.fanyiadrien.messaging

import com.fanyiadrien.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/messaging")
@Tag(name = "Messaging", description = "User messaging and conversation endpoints")
@SecurityRequirement(name = "bearerAuth") // Apply to all methods in this controller
class MessagingController(
    private val messagingService: MessagingService,
    private val authService: AuthService
) {

    @PostMapping("/conversations", produces = ["application/json"])
    @Operation(summary = "Start or get an existing conversation",
        description = "Initiates a new conversation or retrieves an existing one between two users, optionally linked to a listing.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Conversation retrieved or created successfully"),
            ApiResponse(responseCode = "401", description = "Unauthorized, invalid or expired token"),
            ApiResponse(responseCode = "400", description = "Invalid request parameters")
        ]
    )
    fun startConversation(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: StartConversationRequest
    ): ResponseEntity<ConversationView> {
        val requester = resolveUser(token)
        val conversation = messagingService.getOrCreateConversation(
            initiatorId = requester.id,
            otherUserId = request.otherUserId,
            listingId = request.listingId
        )
        return ResponseEntity.ok(conversation)
    }

    @PostMapping("/conversations/{conversationId}/messages", produces = ["application/json"])
    @Operation(summary = "Send a message within a conversation",
        description = "Sends a new message to a specified conversation.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Message sent successfully"),
            ApiResponse(responseCode = "401", description = "Unauthorized, invalid or expired token"),
            ApiResponse(responseCode = "403", description = "Forbidden, user not part of the conversation"),
            ApiResponse(responseCode = "404", description = "Conversation not found")
        ]
    )
    fun sendMessage(
        @RequestHeader("Authorization") token: String,
        @PathVariable conversationId: UUID,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<MessageView> {
        val requester = resolveUser(token)
        val message = messagingService.sendMessage(
            conversationId = conversationId,
            senderId = requester.id,
            content = request.content
        )
        return ResponseEntity.ok(message)
    }

    @GetMapping("/conversations/{conversationId}/messages", produces = ["application/json"])
    @Operation(summary = "Get all messages in a conversation",
        description = "Retrieves the history of messages for a given conversation.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            ApiResponse(responseCode = "401", description = "Unauthorized, invalid or expired token"),
            ApiResponse(responseCode = "403", description = "Forbidden, user not part of the conversation"),
            ApiResponse(responseCode = "404", description = "Conversation not found")
        ]
    )
    fun getMessages(
        @RequestHeader("Authorization") token: String,
        @PathVariable conversationId: UUID
    ): ResponseEntity<List<MessageView>> {
        val requester = resolveUser(token)
        val messages = messagingService.getMessages(conversationId, requester.id)
        return ResponseEntity.ok(messages)
    }

    @GetMapping("/conversations", produces = ["application/json"])
    @Operation(summary = "Get all conversations for the current user",
        description = "Retrieves a list of all conversations the authenticated user is part of.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
            ApiResponse(responseCode = "401", description = "Unauthorized, invalid or expired token")
        ]
    )
    fun getMyConversations(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<List<ConversationView>> {
        val requester = resolveUser(token)
        val conversations = messagingService.getConversationsForUser(requester.id)
        return ResponseEntity.ok(conversations)
    }

    private fun resolveUser(token: String) =
        authService.validateToken(token.removePrefix("Bearer "))
            ?: throw IllegalArgumentException("Invalid or expired token")
}

data class StartConversationRequest(
    val otherUserId: UUID,
    val listingId: UUID? = null
)

data class SendMessageRequest(
    val content: String
)
