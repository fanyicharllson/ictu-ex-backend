package com.fanyiadrien.sync.internal

import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.events.ProductPostedEvent
import com.fanyiadrien.shared.events.UserRegisteredEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class SyncEventConsumer(
    private val syncRepository: SyncRepository,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(topics = [KafkaTopics.USER_REGISTERED], groupId = "sync-group")
    fun consumeUserRegisteredEvent(event: UserRegisteredEvent) {
        val syncRecord = SyncRecord(
            userId = event.userId,
            entityType = "USER",
            entityId = event.userId, // For user, entityId is the userId itself
            action = "CREATED",
            payload = objectMapper.writeValueAsString(event),
            createdAt = Instant.now()
        )
        syncRepository.save(syncRecord)
    }

    @KafkaListener(topics = [KafkaTopics.PRODUCT_POSTED], groupId = "sync-group")
    fun consumeProductPostedEvent(event: ProductPostedEvent) {
        val syncRecord = SyncRecord(
            userId = event.sellerId,
            entityType = "LISTING",
            entityId = event.listingId,
            action = "CREATED",
            payload = objectMapper.writeValueAsString(event),
            createdAt = Instant.now()
        )
        syncRepository.save(syncRecord)
    }

    @KafkaListener(topics = [KafkaTopics.MESSAGE_SENT], groupId = "sync-group")
    fun consumeMessageSentEvent(event: MessageSentEvent) {
        val syncRecord = SyncRecord(
            userId = event.senderId, // Assuming sender is the primary user for this sync record
            entityType = "MESSAGE",
            entityId = event.messageId,
            action = "CREATED",
            payload = objectMapper.writeValueAsString(event),
            createdAt = Instant.now()
        )
        syncRepository.save(syncRecord)
        
        // Also create a record for the receiver if they are different
        if (event.senderId != event.receiverId) {
            val receiverSyncRecord = SyncRecord(
                userId = event.receiverId,
                entityType = "MESSAGE",
                entityId = event.messageId,
                action = "CREATED",
                payload = objectMapper.writeValueAsString(event),
                createdAt = Instant.now()
            )
            syncRepository.save(receiverSyncRecord)
        }
    }
}
