package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.UserRegisteredEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
internal class WelcomeEmailConsumer(
    private val objectMapper: ObjectMapper,
    private val emailService: EmailService
) {

    @KafkaListener(
        topics = [KafkaTopics.USER_REGISTERED],
        groupId = "notification-service"
    )
    fun handleUserRegistered(message: String) {
        val event = objectMapper.readValue(message, UserRegisteredEvent::class.java)
        emailService.sendWelcomeEmail(
            to = event.email,
            displayName = event.displayName
        )
    }
}