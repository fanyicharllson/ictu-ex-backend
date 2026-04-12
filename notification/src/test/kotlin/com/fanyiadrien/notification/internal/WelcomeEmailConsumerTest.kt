package com.fanyiadrien.notification.internal

import com.fanyiadrien.shared.events.UserRegisteredEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class WelcomeEmailConsumerTest {

    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val emailService: EmailService = mock()
    private val consumer = WelcomeEmailConsumer(objectMapper, emailService)

    @Test
    fun `handleUserRegistered calls emailService with correct data`() {
        val event = UserRegisteredEvent(
            userId = UUID.randomUUID(),
            email = "test@ictuniversity.edu.cm",
            displayName = "Test Student",
            studentId = "ICT001"
        )
        val message = objectMapper.writeValueAsString(event)

        consumer.handleUserRegistered(message)

        verify(emailService).sendWelcomeEmail(
            to = event.email,
            displayName = event.displayName
        )
    }
}