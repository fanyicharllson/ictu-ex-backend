package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.VerificationCodeGeneratedEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
internal class VerificationCodeEmailConsumer(
    private val objectMapper: ObjectMapper,
    private val emailService: EmailService
) {

    @KafkaListener(
        topics = [KafkaTopics.VERIFICATION_CODE_GENERATED],
        groupId = "notification-service"
    )
    fun handleVerificationCodeGenerated(message: String) {
        val event = objectMapper.readValue(message, VerificationCodeGeneratedEvent::class.java)
        emailService.sendVerificationCodeEmail(
            to = event.email,
            displayName = event.displayName,
            code = event.code
        )
    }
}

