package com.fanyiadrien.notification.internal

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class EmailService(
    @Value("\${resend.api.key}") private val apiKey: String
) {

    fun sendWelcomeEmail(to: String, displayName: String) {
        val resend = Resend(apiKey)
        val params = CreateEmailOptions.builder()
            .from("ICTU-Ex <noreply@teamnest.me>")
            .to(to)
            .subject("Welcome to ICTU-Ex Marketplace!")
            .html(welcomeEmailTemplate(displayName))
            .build()

        resend.emails().send(params)
    }

    private fun welcomeEmailTemplate(name: String): String = """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <h1 style="color: #2563eb;">Welcome to ICTU-Exchange! 🎓</h1>
            <p>Hi <strong>$name</strong>,</p>
            <p>You've successfully joined the ICTU Smart Student Marketplace.</p>
            <p>You can now:</p>
            <ul>
                <li>Buy and sell academic resources</li>
                <li>Swap textbooks with fellow students</li>
                <li>Connect with your campus community</li>
            </ul>
            <p style="color: #6b7280;">ICT University Cameroon</p>
        </div>
    """.trimIndent()
}