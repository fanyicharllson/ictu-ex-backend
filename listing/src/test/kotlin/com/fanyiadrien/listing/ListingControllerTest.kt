package com.fanyiadrien.listing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ListingControllerTest {

    private val sellerId = UUID.randomUUID()
    private val service = FakeListingService()
    private val controller = ListingController(service)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `createListing returns created listing and passes authenticated seller id`() {
        setAuthenticatedUser(sellerId)
        val request = CreateListingRequest(
            title = "Kotlin Book",
            description = "A useful textbook",
            price = BigDecimal("25.00"),
            category = "TEXTBOOK",
            condition = "GOOD"
        )

        val response = controller.createListing(request)

        assertEquals(201, response.statusCode.value())
        assertEquals("Kotlin Book", response.body?.title)
        assertEquals(sellerId, service.lastCreateSellerId)
        assertEquals(request, service.lastCreateRequest)
    }

    @Test
    fun `getAllListings returns ok with service result`() {
        val listing = sampleListing()
        service.allListings = listOf(listing)

        val response = controller.getAllListings()

        assertEquals(200, response.statusCode.value())
        assertEquals(listOf(listing), response.body)
    }

    @Test
    fun `getListingById returns ok with service result`() {
        val listing = sampleListing()
        service.listingById = listing

        val response = controller.getListingById(listing.id)

        assertEquals(200, response.statusCode.value())
        assertEquals(listing, response.body)
    }

    @Test
    fun `updateListing returns ok and uses authenticated seller id`() {
        setAuthenticatedUser(sellerId)
        val listing = sampleListing()
        service.updatedListing = listing.copy(title = "Updated title")
        val request = UpdateListingRequest(
            title = "Updated title",
            description = null,
            price = null,
            category = null,
            condition = null,
            status = null,
            imageUrls = null
        )

        val response = controller.updateListing(listing.id, request)

        assertEquals(200, response.statusCode.value())
        assertEquals("Updated title", response.body?.title)
        assertEquals(sellerId, service.lastUpdateSellerId)
        assertEquals(request, service.lastUpdateRequest)
    }

    @Test
    fun `deleteListing returns no content and uses authenticated seller id`() {
        setAuthenticatedUser(sellerId)
        val listingId = UUID.randomUUID()

        val response = controller.deleteListing(listingId)

        assertEquals(204, response.statusCode.value())
        assertEquals(listingId, service.lastDeleteId)
        assertEquals(sellerId, service.lastDeleteSellerId)
    }

    @Test
    fun `searchListings returns ok with service result`() {
        val listing = sampleListing()
        service.searchResult = listOf(listing)

        val response = controller.searchListings("kotlin", "TEXTBOOK")

        assertEquals(200, response.statusCode.value())
        assertEquals(listOf(listing), response.body)
        assertEquals("kotlin", service.lastSearchTitle)
        assertEquals("TEXTBOOK", service.lastSearchCategory)
    }

    private fun setAuthenticatedUser(userId: UUID) {
        val authentication = UsernamePasswordAuthenticationToken(userId, "ignored")
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun sampleListing(): Listing = Listing(
        id = UUID.randomUUID(),
        title = "Kotlin Book",
        description = "A useful textbook",
        price = BigDecimal("25.00"),
        category = "TEXTBOOK",
        condition = "GOOD",
        sellerId = sellerId,
        status = "ACTIVE",
        imageUrls = listOf("https://example.com/image.jpg"),
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )


    private class FakeListingService : ListingService {
        var lastCreateRequest: CreateListingRequest? = null
        var lastCreateSellerId: UUID? = null
        var allListings: List<Listing> = emptyList()
        var listingById: Listing? = null
        var updatedListing: Listing? = null
        var lastUpdateRequest: UpdateListingRequest? = null
        var lastUpdateSellerId: UUID? = null
        var lastDeleteId: UUID? = null
        var lastDeleteSellerId: UUID? = null
        var lastSearchTitle: String? = null
        var lastSearchCategory: String? = null
        var searchResult: List<Listing> = emptyList()

        override fun createListing(request: CreateListingRequest, sellerId: UUID): Listing {
            lastCreateRequest = request
            lastCreateSellerId = sellerId
            return listingById ?: sampleListingForCreate(sellerId, request)
        }

        override fun getAllActiveListings(): List<Listing> = allListings

        override fun getListingById(id: UUID): Listing = listingById ?: error("No listing configured for id $id")

        override fun updateListing(id: UUID, request: UpdateListingRequest, sellerId: UUID): Listing {
            lastUpdateRequest = request
            lastUpdateSellerId = sellerId
            return updatedListing ?: error("No updated listing configured for id $id")
        }

        override fun deleteListing(id: UUID, sellerId: UUID) {
            lastDeleteId = id
            lastDeleteSellerId = sellerId
        }

        override fun searchListings(title: String?, category: String?): List<Listing> {
            lastSearchTitle = title
            lastSearchCategory = category
            return searchResult
        }

        override suspend fun analyzeImage(base64Image: String, mimeType: String): AIListingSuggestion {
            error("Not used in this test")
        }

        private fun sampleListingForCreate(sellerId: UUID, request: CreateListingRequest): Listing = Listing(
            id = UUID.randomUUID(),
            title = request.title,
            description = request.description,
            price = request.price,
            category = request.category,
            condition = request.condition,
            sellerId = sellerId,
            status = "ACTIVE",
            imageUrls = request.imageUrls,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

