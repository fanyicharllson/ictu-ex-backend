package com.fanyiadrien.listing.internal

import com.fanyiadrien.listing.CreateListingRequest
import com.fanyiadrien.listing.UpdateListingRequest
import com.fanyiadrien.shared.events.ProductPostedEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import com.fanyiadrien.shared.redis.CacheService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ListingServiceImplTest {

    private val listingRepository: ListingRepository = mock()
    private val eventPublisher: EventPublisher = mock()
    private val cacheService: CacheService = mock()
    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private lateinit var listingService: ListingServiceImpl

    @BeforeEach
    fun setup() {
        listingService = ListingServiceImpl(listingRepository, eventPublisher, cacheService, objectMapper)
        // Default: cache always misses
        whenever(cacheService.get(any())).thenReturn(null)
    }

    // ==================== CREATE LISTING ====================

    @Test
    fun `createListing saves entity and returns listing`() {
        val sellerId = UUID.randomUUID()
        val request = buildCreateRequest()
        val saved = buildEntity(sellerId = sellerId)

        whenever(listingRepository.save(any())).thenReturn(saved)

        val result = listingService.createListing(request, sellerId)

        assertNotNull(result)
        assertEquals(saved.id, result.id)
        assertEquals(saved.title, result.title)
        assertEquals(sellerId, result.sellerId)
        verify(listingRepository).save(any())
    }

    @Test
    fun `createListing publishes ProductPostedEvent to Kafka`() {
        val sellerId = UUID.randomUUID()
        val saved = buildEntity(sellerId = sellerId)
        whenever(listingRepository.save(any())).thenReturn(saved)

        listingService.createListing(buildCreateRequest(), sellerId)

        verify(eventPublisher).publish(eq(KafkaTopics.PRODUCT_POSTED), any<ProductPostedEvent>())
    }

    @Test
    fun `createListing evicts active listings cache`() {
        val sellerId = UUID.randomUUID()
        whenever(listingRepository.save(any())).thenReturn(buildEntity(sellerId = sellerId))

        listingService.createListing(buildCreateRequest(), sellerId)

        verify(cacheService).evictByPattern("listings:*")
    }

    @Test
    fun `createListing throws for invalid category`() {
        val exception = assertThrows<IllegalArgumentException> {
            listingService.createListing(buildCreateRequest(category = "INVALID"), UUID.randomUUID())
        }
        assertTrue(exception.message!!.contains("Invalid category"))
        verify(listingRepository, never()).save(any())
    }

    @Test
    fun `createListing throws for invalid condition`() {
        val exception = assertThrows<IllegalArgumentException> {
            listingService.createListing(buildCreateRequest(condition = "BROKEN"), UUID.randomUUID())
        }
        assertTrue(exception.message!!.contains("Invalid condition"))
        verify(listingRepository, never()).save(any())
    }

    // ==================== GET ALL ACTIVE LISTINGS ====================

    @Test
    fun `getAllActiveListings returns listings from repository when cache is empty`() {
        val entities = listOf(buildEntity(), buildEntity())
        whenever(listingRepository.findByStatus(ListingStatus.ACTIVE)).thenReturn(entities)

        val result = listingService.getAllActiveListings()

        assertEquals(2, result.size)
        verify(listingRepository).findByStatus(ListingStatus.ACTIVE)
        verify(cacheService).set(eq("listings:active"), any())
    }

    @Test
    fun `getAllActiveListings returns cached result when cache hit`() {
        val listings = listOf(buildEntity().let {
            com.fanyiadrien.listing.Listing(
                id = it.id!!, title = it.title, description = it.description,
                price = it.price, category = it.category.name, condition = it.condition.name,
                sellerId = it.sellerId, status = it.status.name, imageUrls = emptyList(),
                createdAt = it.createdAt, updatedAt = it.updatedAt
            )
        })
        val json = objectMapper.writeValueAsString(listings)
        whenever(cacheService.get("listings:active")).thenReturn(json)

        val result = listingService.getAllActiveListings()

        assertEquals(1, result.size)
        verify(listingRepository, never()).findByStatus(any())
    }

    // ==================== GET LISTING BY ID ====================

    @Test
    fun `getListingById returns listing when found`() {
        val entity = buildEntity()
        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))

        val result = listingService.getListingById(entity.id)

        assertEquals(entity.id, result.id)
        verify(cacheService).set(eq("listings:${entity.id}"), any())
    }

    @Test
    fun `getListingById throws NoSuchElementException when not found`() {
        val id = UUID.randomUUID()
        whenever(listingRepository.findById(id)).thenReturn(Optional.empty())

        val exception = assertThrows<NoSuchElementException> {
            listingService.getListingById(id)
        }
        assertTrue(exception.message!!.contains(id.toString()))
    }

    @Test
    fun `getListingById returns cached result when cache hit`() {
        val entity = buildEntity()
        val listing = com.fanyiadrien.listing.Listing(
            id = entity.id!!, title = entity.title, description = entity.description,
            price = entity.price, category = entity.category.name, condition = entity.condition.name,
            sellerId = entity.sellerId, status = entity.status.name, imageUrls = emptyList(),
            createdAt = entity.createdAt, updatedAt = entity.updatedAt
        )
        whenever(cacheService.get("listings:${entity.id}")).thenReturn(objectMapper.writeValueAsString(listing))

        val result = listingService.getListingById(entity.id)

        assertEquals(entity.id, result.id)
        verify(listingRepository, never()).findById(any())
    }

    // ==================== UPDATE LISTING ====================

    @Test
    fun `updateListing succeeds when seller is owner`() {
        val sellerId = UUID.randomUUID()
        val entity = buildEntity(sellerId = sellerId)
        val request = UpdateListingRequest(title = "Updated Title", null, null, null, null, null, null)

        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))
        whenever(listingRepository.save(any())).thenAnswer { it.arguments[0] as ListingEntity }

        val result = listingService.updateListing(entity.id, request, sellerId)

        assertEquals("Updated Title", result.title)
        verify(cacheService).evict("listings:${entity.id}")
        verify(cacheService).evictByPattern("listings:active")
    }

    @Test
    fun `updateListing throws when seller is not owner`() {
        val entity = buildEntity(sellerId = UUID.randomUUID())
        val differentSeller = UUID.randomUUID()
        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))

        val exception = assertThrows<IllegalArgumentException> {
            listingService.updateListing(entity.id, UpdateListingRequest(null, null, null, null, null, null, null), differentSeller)
        }
        assertEquals("You are not the owner of this listing", exception.message)
        verify(listingRepository, never()).save(any())
    }

    @Test
    fun `updateListing throws when listing not found`() {
        val id = UUID.randomUUID()
        whenever(listingRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            listingService.updateListing(id, UpdateListingRequest(null, null, null, null, null, null, null), UUID.randomUUID())
        }
    }

    @Test
    fun `updateListing throws for invalid status`() {
        val sellerId = UUID.randomUUID()
        val entity = buildEntity(sellerId = sellerId)
        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))

        val exception = assertThrows<IllegalArgumentException> {
            listingService.updateListing(entity.id, UpdateListingRequest(null, null, null, null, null, "INVALID_STATUS", null), sellerId)
        }
        assertTrue(exception.message!!.contains("Invalid status"))
    }

    // ==================== DELETE LISTING ====================

    @Test
    fun `deleteListing succeeds when seller is owner`() {
        val sellerId = UUID.randomUUID()
        val entity = buildEntity(sellerId = sellerId)
        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))

        listingService.deleteListing(entity.id, sellerId)

        verify(listingRepository).delete(entity)
        verify(cacheService).evict("listings:${entity.id}")
    }

    @Test
    fun `deleteListing throws when seller is not owner`() {
        val entity = buildEntity(sellerId = UUID.randomUUID())
        whenever(listingRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))

        assertThrows<IllegalArgumentException> {
            listingService.deleteListing(entity.id, UUID.randomUUID())
        }
        verify(listingRepository, never()).delete(any())
    }

    @Test
    fun `deleteListing throws when listing not found`() {
        val id = UUID.randomUUID()
        whenever(listingRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            listingService.deleteListing(id, UUID.randomUUID())
        }
    }

    // ==================== SEARCH LISTINGS ====================

    @Test
    fun `searchListings by title returns matching listings`() {
        val entities = listOf(buildEntity(title = "Java Textbook"))
        whenever(listingRepository.findByTitleContainingIgnoreCaseAndStatus("java", ListingStatus.ACTIVE))
            .thenReturn(entities)

        val result = listingService.searchListings(title = "java", category = null)

        assertEquals(1, result.size)
        assertEquals("Java Textbook", result[0].title)
    }

    @Test
    fun `searchListings by category returns matching listings`() {
        val entities = listOf(buildEntity(category = ListingCategory.ELECTRONICS))
        whenever(listingRepository.findByCategoryAndStatus(ListingCategory.ELECTRONICS, ListingStatus.ACTIVE))
            .thenReturn(entities)

        val result = listingService.searchListings(title = null, category = "ELECTRONICS")

        assertEquals(1, result.size)
        assertEquals("ELECTRONICS", result[0].category)
    }

    @Test
    fun `searchListings by title and category filters both`() {
        val entities = listOf(buildEntity(title = "Laptop", category = ListingCategory.ELECTRONICS))
        whenever(listingRepository.findByTitleContainingIgnoreCaseAndStatus("laptop", ListingStatus.ACTIVE))
            .thenReturn(entities)

        val result = listingService.searchListings(title = "laptop", category = "ELECTRONICS")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchListings with no params returns all active listings`() {
        val entities = listOf(buildEntity(), buildEntity())
        whenever(listingRepository.findByStatus(ListingStatus.ACTIVE)).thenReturn(entities)

        val result = listingService.searchListings(title = null, category = null)

        assertEquals(2, result.size)
    }

    @Test
    fun `searchListings throws for invalid category`() {
        assertThrows<IllegalArgumentException> {
            listingService.searchListings(title = null, category = "UNKNOWN")
        }
    }

    // ==================== HELPERS ====================

    private fun buildCreateRequest(
        category: String = "TEXTBOOK",
        condition: String = "GOOD"
    ) = CreateListingRequest(
        title = "Test Listing",
        description = "A test description",
        price = BigDecimal("5000.00"),
        category = category,
        condition = condition,
        imageUrls = listOf("https://example.com/img.jpg")
    )

    private fun buildEntity(
        id: UUID = UUID.randomUUID(),
        title: String = "Test Listing",
        sellerId: UUID = UUID.randomUUID(),
        category: ListingCategory = ListingCategory.TEXTBOOK,
        status: ListingStatus = ListingStatus.ACTIVE
    ) = ListingEntity(
        id = id,
        title = title,
        description = "A test description",
        price = BigDecimal("5000.00"),
        category = category,
        condition = ListingCondition.GOOD,
        sellerId = sellerId,
        status = status
    )
}
