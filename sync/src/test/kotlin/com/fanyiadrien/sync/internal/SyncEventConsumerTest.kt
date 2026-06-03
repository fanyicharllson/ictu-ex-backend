package com.fanyiadrien.sync.internal

import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.events.ProductPostedEvent
import com.fanyiadrien.shared.events.UserRegisteredEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class SyncEventConsumerTest {

    private val syncRepository: SyncRepository = mock()
    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private lateinit var consumer: SyncEventConsumer

    @BeforeEach
    fun setup() {
        consumer = SyncEventConsumer(syncRepository, objectMapper)
    }

    @Test
    fun `consumeUserRegisteredEvent saves sync record with correct fields`() {
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(userId = userId, email = "john@ictuniversity.edu.cm", displayName = "John", studentId = "ICT001")

        consumer.consumeUserRegisteredEvent(event)

        val captor = argumentCaptor<SyncRecord>()
        verify(syncRepository).save(captor.capture())
        val record = captor.firstValue
        assert(record.userId == userId)
        assert(record.entityType == "USER")
        assert(record.entityId == userId)
        assert(record.action == "CREATED")
        assert(record.payload != null)
    }

    @Test
    fun `consumeProductPostedEvent saves sync record for seller`() {
        val sellerId = UUID.randomUUID()
        val listingId = UUID.randomUUID()
        val event = ProductPostedEvent(
            listingId = listingId,
            sellerId = sellerId,
            title = "Used Laptop",
            category = "ELECTRONICS"
        )

        consumer.consumeProductPostedEvent(event)

        val captor = argumentCaptor<SyncRecord>()
        verify(syncRepository).save(captor.capture())
        val record = captor.firstValue
        assert(record.userId == sellerId)
        assert(record.entityType == "LISTING")
        assert(record.entityId == listingId)
        assert(record.action == "CREATED")
    }

    @Test
    fun `consumeMessageSentEvent saves record for sender`() {
        val senderId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val event = MessageSentEvent(
            messageId = messageId,
            conversationId = UUID.randomUUID(),
            senderId = senderId,
            senderName = "Alice",
            receiverId = receiverId,
            receiverName = "Bob",
            receiverEmail = "bob@ictuniversity.edu.cm",
            content = "Hello!"
        )

        consumer.consumeMessageSentEvent(event)

        val captor = argumentCaptor<SyncRecord>()
        verify(syncRepository, times(2)).save(captor.capture())

        val records = captor.allValues
        val senderRecord = records.first { it.userId == senderId }
        assert(senderRecord.entityType == "MESSAGE")
        assert(senderRecord.entityId == messageId)
        assert(senderRecord.action == "CREATED")
    }

    @Test
    fun `consumeMessageSentEvent saves record for receiver when different from sender`() {
        val senderId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val event = MessageSentEvent(
            messageId = messageId,
            conversationId = UUID.randomUUID(),
            senderId = senderId,
            senderName = "Alice",
            receiverId = receiverId,
            receiverName = "Bob",
            receiverEmail = "bob@ictuniversity.edu.cm",
            content = "Hello!"
        )

        consumer.consumeMessageSentEvent(event)

        val captor = argumentCaptor<SyncRecord>()
        verify(syncRepository, times(2)).save(captor.capture())

        val records = captor.allValues
        val receiverRecord = records.first { it.userId == receiverId }
        assert(receiverRecord.entityType == "MESSAGE")
        assert(receiverRecord.entityId == messageId)
    }

    @Test
    fun `consumeMessageSentEvent saves only one record when sender equals receiver`() {
        val userId = UUID.randomUUID()
        val event = MessageSentEvent(
            messageId = UUID.randomUUID(),
            conversationId = UUID.randomUUID(),
            senderId = userId,
            senderName = "Alice",
            receiverId = userId,
            receiverName = "Alice",
            receiverEmail = "alice@ictuniversity.edu.cm",
            content = "Note to self"
        )

        consumer.consumeMessageSentEvent(event)

        verify(syncRepository, times(1)).save(any())
    }
}
