package com.fanyiadrien.listing.internal

import com.fanyiadrien.shared.events.ImageUploadEvent
import com.fanyiadrien.shared.kafka.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant // Import Instant for updatedAt

@Component
internal class ImageUploadConsumer(
    private val listingRepository: ListingRepository,
    private val cloudinaryService: CloudinaryService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.IMAGE_UPLOAD_REQUESTED], groupId = "listing-image-upload-group")
    @Transactional
    fun consumeImageUploadEvent(event: ImageUploadEvent) {
        log.debug("Received ImageUploadEvent for listingId: {}, imageIndex: {}", event.listingId, event.imageIndex)

        val listing = listingRepository.findById(event.listingId).orElse(null)
        if (listing == null) {
            log.warn("Listing with ID {} not found for image upload. Skipping.", event.listingId)
            return
        }

        if (event.imageIndex < 0 || event.imageIndex >= listing.imageUrls.size) {
            log.warn("Invalid imageIndex {} for listing {}. Skipping image upload.", event.imageIndex, event.listingId)
            return
        }

        try {
            val uploadedUrl = cloudinaryService.uploadImage(event.originalImageUrl)
            
            // Directly modify the mutable list of the managed entity
            listing.imageUrls[event.imageIndex] = uploadedUrl
            listing.updatedAt = Instant.now() // Update the timestamp

            listingRepository.save(listing) // Save the managed entity with updated list
            log.debug("Image uploaded and listing updated for listingId: {}, new URL: {}", event.listingId, uploadedUrl)
        } catch (e: Exception) {
            log.error("Error uploading image for listingId: {}, imageIndex: {}. Error: {}", event.listingId, event.imageIndex, e.message)
            // Depending on requirements, you might want to re-queue the event or send a notification
        }
    }
}
