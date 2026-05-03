package com.fanyiadrien.messaging.internal

import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.messaging.ConversationView
import com.fanyiadrien.messaging.MessageView
import com.fanyiadrien.messaging.MessagingService
import com.fanyiadrien.messaging.internal.persistence.ConversationEntity
import com.fanyiadrien.messaging.internal.persistence.ConversationRepository
import com.fanyiadrien.messaging.internal.persistence.MessageEntity
import com.fanyiadrien.messaging.internal.persistence.MessageRepository
import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class MessagingServiceImpl(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val eventPublisher: EventPublisher,
    private val authService: AuthService
) : MessagingService {

    override fun getOrCreateConversation(
        initiatorId: UUID,
        otherUserId: UUID,
        listingId: UUID?
    ): ConversationView {
        if (initiatorId == otherUserId)
            throw IllegalArgumentException("Cannot start a conversation with yourself")

        // Store with consistent ordering so (A,B) and (B,A) resolve to the same row
        val (first, second) = if (initiatorId.toString() < otherUserId.toString()) initiatorId to otherUserId
                              else otherUserId to initiatorId

        val existing = if (listingId == null)
            conversationRepository.findByParticipantAAndParticipantBAndListingIdIsNull(first, second)
        else
            conversationRepository.findByParticipantAAndParticipantBAndListingId(first, second, listingId)

        val entity = existing ?: conversationRepository.save(
            ConversationEntity(participantA = first, participantB = second, listingId = listingId)
        )
        return entity.toView()
    }

    @Transactional
    override fun sendMessage(conversationId: UUID, senderId: UUID, content: String): MessageView {
        val conversation = conversationRepository.findById(conversationId).orElseThrow {
            IllegalArgumentException("Conversation not found")
        }

        if (!conversation.hasParticipant(senderId))
            throw IllegalArgumentException("You are not a participant of this conversation")

        if (content.isBlank())
            throw IllegalArgumentException("Message content cannot be empty")

        val receiverId = conversation.otherParticipant(senderId)

        val message = messageRepository.save(
            MessageEntity(
                conversationId = conversationId,
                senderId = senderId,
                content = content.trim()
            )
        )

        // Fetch sender and receiver details to enrich the event
        val sender = authService.getUserById(senderId)
            ?: throw IllegalStateException("Sender not found")
        val receiver = authService.getUserById(receiverId)
            ?: throw IllegalStateException("Receiver not found")

        // Event publishing
        eventPublisher.publish(
            topic = KafkaTopics.MESSAGE_SENT,
            event = MessageSentEvent(
                messageId = message.id!!,
                conversationId = conversationId,
                senderId = senderId,
                senderName = sender.displayName,
                receiverId = receiverId,
                receiverName = receiver.displayName,
                receiverEmail = receiver.email,
                content = content.trim()
            )
        )

        return message.toView()
    }

    override fun getMessages(conversationId: UUID, requesterId: UUID): List<MessageView> {
        val conversation = conversationRepository.findById(conversationId).orElseThrow {
            IllegalArgumentException("Conversation not found")
        }

        if (!conversation.hasParticipant(requesterId))
            throw IllegalArgumentException("You are not a participant of this conversation")

        return messageRepository
            .findByConversationIdOrderBySentAtAsc(conversationId)
            .map { it.toView() }
    }

    override fun getConversationsForUser(userId: UUID): List<ConversationView> =
        conversationRepository
            .findByParticipantAOrParticipantB(userId, userId)
            .map { it.toView() }

    private fun ConversationEntity.hasParticipant(userId: UUID) =
        participantA == userId || participantB == userId

    private fun ConversationEntity.otherParticipant(userId: UUID) =
        if (participantA == userId) participantB else participantA

    private fun ConversationEntity.toView() = ConversationView(
        id = id!!,
        participantA = participantA,
        participantB = participantB,
        listingId = listingId,
        createdAt = createdAt
    )

    private fun MessageEntity.toView() = MessageView(
        id = id!!,
        conversationId = conversationId,
        senderId = senderId,
        content = content,
        sentAt = sentAt
    )
}
