package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
internal class EmailMessageConsumer(
    private val emailService: EmailService
) {

    @KafkaListener(topics = [KafkaTopics.MESSAGE_SENT], groupId = "notification-group")
    fun consumeMessageEvent(event: MessageSentEvent) {
        emailService.sendMessageNotificationEmail(
            to = event.receiverEmail,
            receiverName = event.receiverName,
            senderName = event.senderName,
            messageSnippet = event.content.take(50)
        )
    }
}
