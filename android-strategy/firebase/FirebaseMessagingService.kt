package com.fanyiadrien.ictu_ex.data.remote.firebase

import com.fanyiadrien.ictu_ex.data.remote.api.Conversation
import com.fanyiadrien.ictu_ex.data.remote.api.Message
import com.fanyiadrien.ictu_ex.data.remote.api.MessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * STEP 2 — FIREBASE MESSAGING IMPLEMENTATION
 *
 * Reads/writes conversations and messages from Firestore.
 */
class FirebaseMessagingService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : MessagingService {

    private val conversations get() = firestore.collection("conversations")
    private val messages      get() = firestore.collection("messages")

    override suspend fun getOrCreateConversation(
        otherUserId: String,
        listingId: String?
    ): Result<Conversation> = runCatching {
        val myId = currentUserId()

        // Consistent ordering so (A,B) == (B,A)
        val (participantA, participantB) = if (myId < otherUserId) myId to otherUserId
                                          else otherUserId to myId

        // Try to find existing
        var query = conversations
            .whereEqualTo("participantA", participantA)
            .whereEqualTo("participantB", participantB)
        if (listingId != null) query = query.whereEqualTo("listingId", listingId)

        val existing = query.get().await().documents.firstOrNull()
        if (existing != null) return@runCatching existing.toConversation()

        // Create new
        val doc = conversations.document()
        val now = System.currentTimeMillis().toString()
        val data = mutableMapOf<String, Any>(
            "id"           to doc.id,
            "participantA" to participantA,
            "participantB" to participantB,
            "createdAt"    to now
        )
        if (listingId != null) data["listingId"] = listingId
        doc.set(data).await()
        Conversation(
            id = doc.id, participantA = participantA, participantB = participantB,
            listingId = listingId, createdAt = now
        )
    }

    override suspend fun sendMessage(conversationId: String, content: String): Result<Message> = runCatching {
        val senderId = currentUserId()
        val doc = messages.document()
        val now = System.currentTimeMillis().toString()
        val data = mapOf(
            "id"             to doc.id,
            "conversationId" to conversationId,
            "senderId"       to senderId,
            "content"        to content,
            "sentAt"         to now
        )
        doc.set(data).await()
        Message(id = doc.id, conversationId = conversationId, senderId = senderId, content = content, sentAt = now)
    }

    override suspend fun getMessages(conversationId: String): Result<List<Message>> = runCatching {
        messages
            .whereEqualTo("conversationId", conversationId)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .get().await()
            .documents.map { doc ->
                Message(
                    id             = doc.id,
                    conversationId = doc.getString("conversationId") ?: "",
                    senderId       = doc.getString("senderId") ?: "",
                    content        = doc.getString("content") ?: "",
                    sentAt         = doc.getString("sentAt") ?: ""
                )
            }
    }

    override suspend fun getMyConversations(): Result<List<Conversation>> = runCatching {
        val myId = currentUserId()
        val asA = conversations.whereEqualTo("participantA", myId).get().await().documents
        val asB = conversations.whereEqualTo("participantB", myId).get().await().documents
        (asA + asB).distinctBy { it.id }.map { it.toConversation() }
    }

    private fun currentUserId() = firebaseAuth.currentUser?.uid
        ?: throw IllegalStateException("Not authenticated")

    private fun com.google.firebase.firestore.DocumentSnapshot.toConversation() = Conversation(
        id           = id,
        participantA = getString("participantA") ?: "",
        participantB = getString("participantB") ?: "",
        listingId    = getString("listingId"),
        createdAt    = getString("createdAt") ?: ""
    )
}
