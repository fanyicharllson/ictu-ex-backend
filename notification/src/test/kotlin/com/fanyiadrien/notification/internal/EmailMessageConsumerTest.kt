package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.MessageSentEvent
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class EmailMessageConsumerTest {

    private val emailService: EmailService = mock()
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    private lateinit var consumer: EmailMessageConsumer

    @BeforeEach
    fun setup() {
        consumer = EmailMessageConsumer(emailService, objectMapper)
    }

    @Test
    fun `consumeMessageEvent sends notification email to receiver`() {
        val event = buildEvent(content = "Hey Bob, is the textbook still available?")
        val json = objectMapper.writeValueAsString(event)

        consumer.consumeMessageEvent(json)

        verify(emailService).sendMessageNotificationEmail(
            to = "bob@ictuniversity.edu.cm",
            receiverName = "Bob",
            senderName = "Alice",
            messageSnippet = "Hey Bob, is the textbook still available?"
        )
    }

    @Test
    fun `consumeMessageEvent truncates message snippet to 50 chars`() {
        val longContent = "A".repeat(100)
        val event = buildEvent(content = longContent)
        val json = objectMapper.writeValueAsString(event)

        consumer.consumeMessageEvent(json)

        verify(emailService).sendMessageNotificationEmail(
            to = "bob@ictuniversity.edu.cm",
            receiverName = "Bob",
            senderName = "Alice",
            messageSnippet = "A".repeat(50)
        )
    }

    @Test
    fun `consumeMessageEvent calls emailService exactly once per event`() {
        val event = buildEvent(content = "Hello!")
        consumer.consumeMessageEvent(objectMapper.writeValueAsString(event))

        verify(emailService, times(1)).sendMessageNotificationEmail(any(), any(), any(), any())
    }

    private fun buildEvent(content: String) = MessageSentEvent(
        messageId = UUID.randomUUID(),
        conversationId = UUID.randomUUID(),
        senderId = UUID.randomUUID(),
        senderName = "Alice",
        receiverId = UUID.randomUUID(),
        receiverName = "Bob",
        receiverEmail = "bob@ictuniversity.edu.cm",
        content = content
    )
}
