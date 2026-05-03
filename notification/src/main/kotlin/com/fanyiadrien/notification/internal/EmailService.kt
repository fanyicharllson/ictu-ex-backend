package com.fanyiadrien.notification.internal

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class EmailService(
    @Value("\${resend.api.key}") private val apiKey: String
) {
    private val resend = Resend(apiKey)
    private val defaultFrom = "ICTU-Ex <noreply@teamnest.me>"

    fun sendEmail(to: String, subject: String, htmlContent: String, from: String = defaultFrom) {
        val params = CreateEmailOptions.builder()
            .from(from)
            .to(to)
            .subject(subject)
            .html(htmlContent)
            .build()

        resend.emails().send(params)
    }

    fun sendWelcomeEmail(to: String, displayName: String) {
        val content = """
            <p style="font-size: 16px; line-height: 1.5;">Hi <strong>$displayName</strong>,</p>
            <p style="font-size: 16px; line-height: 1.5;">You've successfully joined the ICTU Smart Student Marketplace. We're thrilled to have you!</p>
            <p style="font-size: 16px; line-height: 1.5;">You can now:</p>
            <ul style="font-size: 16px; line-height: 1.5; padding-left: 20px;">
                <li style="margin-bottom: 8px;">Buy and sell academic resources</li>
                <li style="margin-bottom: 8px;">Swap textbooks with fellow students</li>
                <li style="margin-bottom: 8px;">Connect with your campus community</li>
            </ul>
            <p style="font-size: 16px; line-height: 1.5;">Start exploring by logging into your account and discovering what's available.</p>
            <div style="text-align: center; margin-top: 30px;">
                <a href="https://ictu-ex.teamnest.me/login" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 12px 25px; border-radius: 5px; text-decoration: none; font-size: 16px;">Go to Marketplace</a>
            </div>
        """.trimIndent()
        
        sendEmail(to, "Welcome to ICTU-Ex Marketplace! 🎓", baseLayout("Welcome to ICTU-Exchange!", content))
    }

    fun sendMessageNotificationEmail(to: String, receiverName: String, senderName: String, messageSnippet: String) {
        val content = """
            <p style="font-size: 16px; line-height: 1.5;">Hi <strong>$receiverName</strong>,</p>
            <p style="font-size: 16px; line-height: 1.5;">You have received a new message on the marketplace.</p>
            <div style="background-color: #f3f4f6; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                <p style="margin: 0; font-style: italic; color: #374151;">
                    "<strong>$senderName:</strong> $messageSnippet..."
                </p>
            </div>
            <p style="font-size: 16px; line-height: 1.5;">Click the button below to view the full message and reply.</p>
            <div style="text-align: center; margin-top: 30px;">
                <a href="https://ictu-ex.teamnest.me/messages" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 12px 25px; border-radius: 5px; text-decoration: none; font-size: 16px;">View Message</a>
            </div>
        """.trimIndent()

        sendEmail(to, "New message from $senderName", baseLayout("New Message Received", content))
    }

    private fun baseLayout(title: String, body: String): String = """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
            <div style="background-color: #2563eb; color: #ffffff; padding: 20px; text-align: center;">
                <h1 style="margin: 0; font-size: 24px;">$title</h1>
            </div>
            <div style="padding: 20px;">
                $body
            </div>
            <div style="background-color: #f8f8f8; padding: 20px; text-align: center; font-size: 14px; color: #6b7280;">
                <p style="margin: 0;">&copy; ${java.time.Year.now()} ICT University Cameroon. All rights reserved.</p>
                <p style="margin: 5px 0 0;">If you have any questions, feel free to contact our support team.</p>
            </div>
        </div>
    """.trimIndent()
}