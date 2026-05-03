package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.VerificationCodeGeneratedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class VerificationCodeEmailConsumerTest {

    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val emailService: EmailService = mock()
    private val consumer = VerificationCodeEmailConsumer(objectMapper, emailService)

    @Test
    fun `handleVerificationCodeGenerated calls emailService with correct data`() {
        val event = VerificationCodeGeneratedEvent(
            userId = UUID.randomUUID(),
            email = "test@ictuniversity.edu.cm",
            displayName = "Test Student",
            code = "ICTUEx-123456"
        )
        val message = objectMapper.writeValueAsString(event)

        consumer.handleVerificationCodeGenerated(message)

        verify(emailService).sendVerificationCodeEmail(
            to = event.email,
            displayName = event.displayName,
            code = event.code
        )
    }
}

