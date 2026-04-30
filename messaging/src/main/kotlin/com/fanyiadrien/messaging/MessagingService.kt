package com.fanyiadrien.messaging

import java.util.UUID

interface MessagingService {
    fun getOrCreateConversation(initiatorId: UUID, otherUserId: UUID, listingId: UUID?): ConversationView
    fun sendMessage(conversationId: UUID, senderId: UUID, content: String): MessageView
    fun getMessages(conversationId: UUID, requesterId: UUID): List<MessageView>
    fun getConversationsForUser(userId: UUID): List<ConversationView>
}
