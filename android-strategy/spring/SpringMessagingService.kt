package com.fanyiadrien.ictu_ex.data.remote.spring

import com.fanyiadrien.ictu_ex.data.remote.api.Conversation
import com.fanyiadrien.ictu_ex.data.remote.api.Message
import com.fanyiadrien.ictu_ex.data.remote.api.MessagingService
import javax.inject.Inject

/**
 * STEP 3 — SPRING BOOT MESSAGING IMPLEMENTATION
 */
class SpringMessagingService @Inject constructor(
    private val api: IctuExApiService,
    private val tokenStore: TokenStore
) : MessagingService {

    private fun bearer() = tokenStore.getBearerToken()
        ?: throw IllegalStateException("Not authenticated")

    override suspend fun getOrCreateConversation(
        otherUserId: String,
        listingId: String?
    ): Result<Conversation> = runCatching {
        api.getOrCreateConversation(
            token   = bearer(),
            request = CreateConversationRequest(otherUserId, listingId)
        )
    }

    override suspend fun sendMessage(conversationId: String, content: String): Result<Message> = runCatching {
        api.sendMessage(bearer(), conversationId, SendMessageRequest(content))
    }

    override suspend fun getMessages(conversationId: String): Result<List<Message>> = runCatching {
        api.getMessages(bearer(), conversationId)
    }

    override suspend fun getMyConversations(): Result<List<Conversation>> = runCatching {
        api.getMyConversations(bearer())
    }
}
