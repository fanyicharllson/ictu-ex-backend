package com.fanyiadrien.listing.internal

import com.fanyiadrien.shared.events.ImageUploadEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ImageUploadConsumerTest {

    private val listingRepository: ListingRepository = mock()
    private val cloudinaryService: CloudinaryService = mock()
    private lateinit var consumer: ImageUploadConsumer

    @BeforeEach
    fun setup() {
        consumer = ImageUploadConsumer(listingRepository, cloudinaryService)
    }

    @Test
    fun `consumeImageUploadEvent uploads image and updates listing url`() {
        val listingId = UUID.randomUUID()
        val entity = buildEntity(listingId, imageUrls = mutableListOf("data:image/png;base64,abc"))
        whenever(listingRepository.findById(listingId)).thenReturn(Optional.of(entity))
        whenever(cloudinaryService.uploadImage(any())).thenReturn("https://cdn.cloudinary.com/img.jpg")
        whenever(listingRepository.save(any())).thenReturn(entity)

        val event = ImageUploadEvent(listingId = listingId, originalImageUrl = "data:image/png;base64,abc", imageIndex = 0)
        consumer.consumeImageUploadEvent(event)

        verify(cloudinaryService).uploadImage("data:image/png;base64,abc")
        verify(listingRepository).save(entity)
    }

    @Test
    fun `consumeImageUploadEvent skips when listing not found`() {
        val listingId = UUID.randomUUID()
        whenever(listingRepository.findById(listingId)).thenReturn(Optional.empty())

        val event = ImageUploadEvent(listingId = listingId, originalImageUrl = "http://img.jpg", imageIndex = 0)
        consumer.consumeImageUploadEvent(event)

        verify(cloudinaryService, never()).uploadImage(any())
        verify(listingRepository, never()).save(any())
    }

    @Test
    fun `consumeImageUploadEvent skips when imageIndex is out of bounds`() {
        val listingId = UUID.randomUUID()
        val entity = buildEntity(listingId, imageUrls = mutableListOf("http://img.jpg"))
        whenever(listingRepository.findById(listingId)).thenReturn(Optional.of(entity))

        // index 5 is out of bounds for a list of size 1
        val event = ImageUploadEvent(listingId = listingId, originalImageUrl = "http://img.jpg", imageIndex = 5)
        consumer.consumeImageUploadEvent(event)

        verify(cloudinaryService, never()).uploadImage(any())
        verify(listingRepository, never()).save(any())
    }

    @Test
    fun `consumeImageUploadEvent skips when imageIndex is negative`() {
        val listingId = UUID.randomUUID()
        val entity = buildEntity(listingId, imageUrls = mutableListOf("http://img.jpg"))
        whenever(listingRepository.findById(listingId)).thenReturn(Optional.of(entity))

        val event = ImageUploadEvent(listingId = listingId, originalImageUrl = "http://img.jpg", imageIndex = -1)
        consumer.consumeImageUploadEvent(event)

        verify(cloudinaryService, never()).uploadImage(any())
    }

    @Test
    fun `consumeImageUploadEvent does not rethrow when cloudinary upload fails`() {
        val listingId = UUID.randomUUID()
        val entity = buildEntity(listingId, imageUrls = mutableListOf("http://img.jpg"))
        whenever(listingRepository.findById(listingId)).thenReturn(Optional.of(entity))
        whenever(cloudinaryService.uploadImage(any())).thenThrow(RuntimeException("Cloudinary error"))

        // Should NOT throw — exception is caught internally
        val event = ImageUploadEvent(listingId = listingId, originalImageUrl = "http://img.jpg", imageIndex = 0)
        consumer.consumeImageUploadEvent(event)

        verify(listingRepository, never()).save(any())
    }

    private fun buildEntity(id: UUID, imageUrls: MutableList<String> = mutableListOf()) =
        ListingEntity(
            id = id,
            title = "Test",
            description = "Desc",
            price = BigDecimal("1000"),
            category = ListingCategory.TEXTBOOK,
            condition = ListingCondition.GOOD,
            sellerId = UUID.randomUUID(),
            imageUrls = imageUrls
        )
}
