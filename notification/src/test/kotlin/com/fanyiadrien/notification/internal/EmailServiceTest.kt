package com.fanyiadrien.notification.internal

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.*

class EmailServiceTest {

    // We test EmailService by subclassing and overriding sendEmail so no real HTTP call is made
    private lateinit var capturedTo: String
    private lateinit var capturedSubject: String
    private lateinit var capturedHtml: String

    private lateinit var emailService: EmailService

    @BeforeEach
    fun setup() {
        // Use a spy so we can intercept sendEmail without hitting Resend
        emailService = spy(EmailService("test-api-key"))
        doAnswer { invocation ->
            capturedTo = invocation.arguments[0] as String
            capturedSubject = invocation.arguments[1] as String
            capturedHtml = invocation.arguments[2] as String
            null
        }.whenever(emailService).sendEmail(any(), any(), any(), any())
    }

    @Test
    fun `sendWelcomeEmail calls sendEmail with correct to and subject`() {
        emailService.sendWelcomeEmail("student@ictuniversity.edu.cm", "John Doe")

        verify(emailService).sendEmail(
            eq("student@ictuniversity.edu.cm"),
            eq("Welcome to ICTU-Ex Marketplace! 🎓"),
            any(),
            any()
        )
    }

    @Test
    fun `sendWelcomeEmail includes display name in html content`() {
        emailService.sendWelcomeEmail("student@ictuniversity.edu.cm", "John Doe")

        assert(capturedHtml.contains("John Doe"))
    }

    @Test
    fun `sendVerificationCodeEmail calls sendEmail with correct subject`() {
        emailService.sendVerificationCodeEmail("student@ictuniversity.edu.cm", "Jane", "ICTUEx-123456")

        verify(emailService).sendEmail(
            eq("student@ictuniversity.edu.cm"),
            eq("Your ICTU-Ex verification code"),
            any(),
            any()
        )
    }

    @Test
    fun `sendVerificationCodeEmail includes code in html content`() {
        emailService.sendVerificationCodeEmail("student@ictuniversity.edu.cm", "Jane", "ICTUEx-123456")

        assert(capturedHtml.contains("ICTUEx-123456"))
    }

    @Test
    fun `sendVerificationCodeEmail includes display name in html content`() {
        emailService.sendVerificationCodeEmail("student@ictuniversity.edu.cm", "Jane", "ICTUEx-123456")

        assert(capturedHtml.contains("Jane"))
    }

    @Test
    fun `sendMessageNotificationEmail calls sendEmail with correct subject`() {
        emailService.sendMessageNotificationEmail(
            to = "receiver@ictuniversity.edu.cm",
            receiverName = "Alice",
            senderName = "Bob",
            messageSnippet = "Hey, is the textbook still available?"
        )

        verify(emailService).sendEmail(
            eq("receiver@ictuniversity.edu.cm"),
            eq("New message from Bob"),
            any(),
            any()
        )
    }

    @Test
    fun `sendMessageNotificationEmail includes sender name in html content`() {
        emailService.sendMessageNotificationEmail(
            to = "receiver@ictuniversity.edu.cm",
            receiverName = "Alice",
            senderName = "Bob",
            messageSnippet = "Hey!"
        )

        assert(capturedHtml.contains("Bob"))
    }

    @Test
    fun `sendMessageNotificationEmail includes message snippet in html content`() {
        emailService.sendMessageNotificationEmail(
            to = "receiver@ictuniversity.edu.cm",
            receiverName = "Alice",
            senderName = "Bob",
            messageSnippet = "Unique snippet text"
        )

        assert(capturedHtml.contains("Unique snippet text"))
    }
}
