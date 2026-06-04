package com.fanyiadrien.listing.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ListingEntityTest {

    @Test
    fun `listing entity uses domain defaults`() {
        val entity = ListingEntity(
            title = "  Kotlin Notes  ",
            description = "  Useful study guide  ",
            price = BigDecimal("1200.00"),
            sellerId = UUID.randomUUID()
        )

        assertEquals("  Kotlin Notes  ", entity.title)
        assertEquals("  Useful study guide  ", entity.description)
        assertEquals(ListingCategory.TEXTBOOK, entity.category)
        assertEquals(ListingCondition.GOOD, entity.condition)
        assertEquals(ListingStatus.ACTIVE, entity.status)
        assertTrue(entity.imageUrls.isEmpty())
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    fun `listing entity stores explicit status and image urls`() {
        val createdAt = Instant.parse("2026-06-03T12:00:00Z")
        val updatedAt = Instant.parse("2026-06-03T12:05:00Z")
        val images = mutableListOf("https://example.com/1.jpg", "https://example.com/2.jpg")
        val sellerId = UUID.randomUUID()

        val entity = ListingEntity(
            id = UUID.randomUUID(),
            title = "Laptop",
            description = "Good condition laptop",
            price = BigDecimal("45000.00"),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.FAIR,
            sellerId = sellerId,
            status = ListingStatus.SOLD,
            imageUrls = images,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        assertEquals(ListingCategory.ELECTRONICS, entity.category)
        assertEquals(ListingCondition.FAIR, entity.condition)
        assertEquals(ListingStatus.SOLD, entity.status)
        assertEquals(images, entity.imageUrls)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }
}

