package com.fanyiadrien.ictu_ex.data.remote.api

// ─── Models ──────────────────────────────────────────────────────────────────

data class Conversation(
    val id: String,
    val participantA: String,
    val participantB: String,
    val listingId: String?,
    val createdAt: String
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val sentAt: String
)

// ─── Interface ───────────────────────────────────────────────────────────────

interface MessagingService {
    /** Start or retrieve an existing conversation with another user. */
    suspend fun getOrCreateConversation(
        otherUserId: String,
        listingId: String? = null
    ): Result<Conversation>

    /** Send a message in an existing conversation. */
    suspend fun sendMessage(conversationId: String, content: String): Result<Message>

    /** Get all messages in a conversation (ordered oldest → newest). */
    suspend fun getMessages(conversationId: String): Result<List<Message>>

    /** Get all conversations for the current user. */
    suspend fun getMyConversations(): Result<List<Conversation>>
}
