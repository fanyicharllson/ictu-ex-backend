package com.fanyiadrien.listing.internal

import com.fanyiadrien.listing.CreateListingRequest
import com.fanyiadrien.listing.Listing
import com.fanyiadrien.listing.ListingService
import com.fanyiadrien.listing.UpdateListingRequest
import com.fanyiadrien.shared.events.ProductPostedEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fanyiadrien.shared.redis.CacheService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
internal class ListingServiceImpl(
    private val listingRepository: ListingRepository,
    private val eventPublisher: EventPublisher,
    private val cacheService: CacheService,
    private val objectMapper: ObjectMapper
) : ListingService {

    @PreAuthorize("isAuthenticated()")
    override fun createListing(request: CreateListingRequest, sellerId: UUID): Listing {
        val entity = ListingEntity(
            title = request.title.trim(),
            description = request.description.trim(),
            price = request.price,
            category = parseCategory(request.category),
            condition = parseCondition(request.condition),
            sellerId = sellerId,
            imageUrls = request.imageUrls.toMutableList()
        )
        val saved = listingRepository.save(entity)

        eventPublisher.publish(
            topic = KafkaTopics.PRODUCT_POSTED,
            event = ProductPostedEvent(
                listingId = saved.id!!,
                sellerId = sellerId,
                title = saved.title,
                category = saved.category.name
            )
        )

        cacheService.evictByPattern("listings:*")
        return saved.toListing()
    }

    override fun getAllActiveListings(): List<Listing> {
        val cacheKey = "listings:active"
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return objectMapper.readValue(cached, objectMapper.typeFactory
                .constructCollectionType(List::class.java, Listing::class.java))
        }
        val listings = listingRepository.findByStatus(ListingStatus.ACTIVE).map { it.toListing() }
        cacheService.set(cacheKey, objectMapper.writeValueAsString(listings))
        return listings
    }

    override fun getListingById(id: UUID): Listing {
        val cacheKey = "listings:$id"
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return objectMapper.readValue(cached, Listing::class.java)
        }
        val listing = findOrThrow(id).toListing()
        cacheService.set(cacheKey, objectMapper.writeValueAsString(listing))
        return listing
    }

    @PreAuthorize("isAuthenticated()")
    override fun updateListing(id: UUID, request: UpdateListingRequest, sellerId: UUID): Listing {
        val entity = findOrThrow(id)
        checkOwnership(entity, sellerId)

        val updated = ListingEntity(
            id = entity.id,
            title = request.title?.trim() ?: entity.title,
            description = request.description?.trim() ?: entity.description,
            price = request.price ?: entity.price,
            category = request.category?.let { parseCategory(it) } ?: entity.category,
            condition = request.condition?.let { parseCondition(it) } ?: entity.condition,
            sellerId = entity.sellerId,
            status = request.status?.let { parseStatus(it) } ?: entity.status,
            imageUrls = request.imageUrls?.toMutableList() ?: entity.imageUrls,
            createdAt = entity.createdAt,
            updatedAt = Instant.now()
        )
        val saved = listingRepository.save(updated)

        cacheService.evict("listings:$id")
        cacheService.evictByPattern("listings:active")
        cacheService.evictByPattern("listings:search:*")
        return saved.toListing()
    }

    @PreAuthorize("isAuthenticated()")
    override fun deleteListing(id: UUID, sellerId: UUID) {
        val entity = findOrThrow(id)
        checkOwnership(entity, sellerId)
        listingRepository.delete(entity)

        cacheService.evict("listings:$id")
        cacheService.evictByPattern("listings:active")
        cacheService.evictByPattern("listings:search:*")
    }

    override fun searchListings(title: String?, category: String?): List<Listing> {
        val cacheKey = "listings:search:${title.orEmpty()}:${category.orEmpty()}"
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return objectMapper.readValue(cached, objectMapper.typeFactory
                .constructCollectionType(List::class.java, Listing::class.java))
        }

        val results = when {
            !title.isNullOrBlank() && !category.isNullOrBlank() -> {
                val cat = parseCategory(category)
                listingRepository.findByTitleContainingIgnoreCaseAndStatus(title, ListingStatus.ACTIVE)
                    .filter { it.category == cat }
            }
            !title.isNullOrBlank() ->
                listingRepository.findByTitleContainingIgnoreCaseAndStatus(title, ListingStatus.ACTIVE)
            !category.isNullOrBlank() ->
                listingRepository.findByCategoryAndStatus(parseCategory(category), ListingStatus.ACTIVE)
            else -> listingRepository.findByStatus(ListingStatus.ACTIVE)
        }.map { it.toListing() }

        cacheService.set(cacheKey, objectMapper.writeValueAsString(results))
        return results
    }

    private fun findOrThrow(id: UUID): ListingEntity =
        listingRepository.findById(id).orElseThrow { NoSuchElementException("Listing not found: $id") }

    private fun checkOwnership(entity: ListingEntity, sellerId: UUID) {
        if (entity.sellerId != sellerId)
            throw IllegalArgumentException("You are not the owner of this listing")
    }

    private fun parseCategory(value: String): ListingCategory =
        try { ListingCategory.valueOf(value.trim().uppercase()) }
        catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid category: $value. Valid values: ${ListingCategory.entries.joinToString()}")
        }

    private fun parseCondition(value: String): ListingCondition =
        try { ListingCondition.valueOf(value.trim().uppercase()) }
        catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid condition: $value. Valid values: ${ListingCondition.entries.joinToString()}")
        }

    private fun parseStatus(value: String): ListingStatus =
        try { ListingStatus.valueOf(value.trim().uppercase()) }
        catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $value. Valid values: ${ListingStatus.entries.joinToString()}")
        }

    private fun ListingEntity.toListing() = Listing(
        id = id!!,
        title = title,
        description = description,
        price = price,
        category = category.name,
        condition = condition.name,
        sellerId = sellerId,
        status = status.name,
        imageUrls = imageUrls.toList(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
