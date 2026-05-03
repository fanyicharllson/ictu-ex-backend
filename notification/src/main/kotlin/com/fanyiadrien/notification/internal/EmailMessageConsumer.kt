package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
internal class EmailMessageConsumer(
    private val emailService: EmailService,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(topics = [KafkaTopics.MESSAGE_SENT], groupId = "notification-group")
    fun consumeMessageEvent(message: String) {
        val event = objectMapper.readValue(message, MessageSentEvent::class.java)
        emailService.sendMessageNotificationEmail(
            to = event.receiverEmail,
            receiverName = event.receiverName,
            senderName = event.senderName,
            messageSnippet = event.content.take(50)
        )
    }
}
