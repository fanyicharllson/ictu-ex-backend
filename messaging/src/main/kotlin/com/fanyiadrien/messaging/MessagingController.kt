package com.fanyiadrien.messaging

import com.fanyiadrien.auth.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/messaging")
class MessagingController(
    private val messagingService: MessagingService,
    private val authService: AuthService
) {

    @PostMapping("/conversations")
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

    @PostMapping("/conversations/{conversationId}/messages")
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

    @GetMapping("/conversations/{conversationId}/messages")
    fun getMessages(
        @RequestHeader("Authorization") token: String,
        @PathVariable conversationId: UUID
    ): ResponseEntity<List<MessageView>> {
        val requester = resolveUser(token)
        val messages = messagingService.getMessages(conversationId, requester.id)
        return ResponseEntity.ok(messages)
    }

    @GetMapping("/conversations")
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
