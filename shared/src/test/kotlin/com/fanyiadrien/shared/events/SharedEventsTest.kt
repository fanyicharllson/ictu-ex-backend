package com.fanyiadrien.shared.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SharedEventsTest {

    @Test
    fun `user registered and verified events keep payload and default timestamps`() {
        val userId = UUID.randomUUID()
        val before = Instant.now()
        val registered = UserRegisteredEvent(userId, "student@ictuniversity.edu.cm", "Student", "ICT001")
        val verified = UserVerifiedEvent(userId, "student@ictuniversity.edu.cm", "Student")
        val after = Instant.now()

        assertEquals(userId, registered.userId)
        assertEquals("ICT001", registered.studentId)
        assertTrue(!registered.occurredAt.isBefore(before) && !registered.occurredAt.isAfter(after))

        assertEquals(userId, verified.userId)
        assertEquals("Student", verified.displayName)
        assertTrue(!verified.verifiedAt.isBefore(before) && !verified.verifiedAt.isAfter(after))
    }

    @Test
    fun `product and message events store expected fields`() {
        val listingId = UUID.randomUUID()
        val sellerId = UUID.randomUUID()
        val productEvent = ProductPostedEvent(listingId, sellerId, "Laptop", "ELECTRONICS")

        assertEquals(listingId, productEvent.listingId)
        assertEquals(sellerId, productEvent.sellerId)
        assertEquals("Laptop", productEvent.title)
        assertNotNull(productEvent.occurredAt)

        val messageEvent = MessageSentEvent(
            messageId = UUID.randomUUID(),
            conversationId = UUID.randomUUID(),
            senderId = UUID.randomUUID(),
            senderName = "Alice",
            receiverId = UUID.randomUUID(),
            receiverName = "Bob",
            receiverEmail = "bob@ictuniversity.edu.cm",
            content = "Hi",
            occurredAt = Instant.now()
        )

        assertEquals("Alice", messageEvent.senderName)
        assertEquals("Bob", messageEvent.receiverName)
        assertEquals("Hi", messageEvent.content)
    }

    @Test
    fun `verification and image upload events store explicit payload`() {
        val userId = UUID.randomUUID()
        val verificationEvent = VerificationCodeGeneratedEvent(
            userId = userId,
            email = "student@ictuniversity.edu.cm",
            displayName = "Student",
            code = "123456"
        )

        assertEquals(userId, verificationEvent.userId)
        assertEquals("123456", verificationEvent.code)

        val listingId = UUID.randomUUID()
        val imageEvent = ImageUploadEvent(
            listingId = listingId,
            originalImageUrl = "https://cdn.example.com/image.png",
            imageIndex = 2
        )

        assertEquals(listingId, imageEvent.listingId)
        assertEquals("https://cdn.example.com/image.png", imageEvent.originalImageUrl)
        assertEquals(2, imageEvent.imageIndex)
    }
}

