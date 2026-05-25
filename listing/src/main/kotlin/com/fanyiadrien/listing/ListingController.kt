package com.fanyiadrien.listing

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/api/listings")
@Tag(name = "Listings", description = "Marketplace listing endpoints")
class ListingController(private val listingService: ListingService) {

    companion object {
        private const val IMAGE_EMPTY_ERROR = "Image file cannot be empty."
    }

    @PostMapping
    @Operation(summary = "Create a new marketplace listing")
    @SecurityRequirement(name = "bearerAuth")
    fun createListing(@RequestBody request: CreateListingRequest): ResponseEntity<Listing> {
        val listing = listingService.createListing(request, currentUserId())
        return ResponseEntity.status(HttpStatus.CREATED).body(listing)
    }

    @GetMapping
    @Operation(summary = "Get all active listings")
    @SecurityRequirement(name = "bearerAuth")
    fun getAllListings(): ResponseEntity<List<Listing>> =
        ResponseEntity.ok(listingService.getAllActiveListings())

    @GetMapping("/{id}")
    @Operation(summary = "Get listing details by ID")
    @SecurityRequirement(name = "bearerAuth")
    fun getListingById(@PathVariable id: UUID): ResponseEntity<Listing> =
        ResponseEntity.ok(listingService.getListingById(id))

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing listing")
    @SecurityRequirement(name = "bearerAuth")
    fun updateListing(
        @PathVariable id: UUID,
        @RequestBody request: UpdateListingRequest
    ): ResponseEntity<Listing> =
        ResponseEntity.ok(listingService.updateListing(id, request, currentUserId()))

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a listing")
    @SecurityRequirement(name = "bearerAuth")
    fun deleteListing(@PathVariable id: UUID): ResponseEntity<Unit> {
        listingService.deleteListing(id, currentUserId())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    @Operation(summary = "Search active listings by title and/or category")
    @SecurityRequirement(name = "bearerAuth")
    fun searchListings(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<Listing>> =
        ResponseEntity.ok(listingService.searchListings(title, category))

    @PostMapping("/analyze-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "AI-powered listing analyzer",
        description = "Upload item image, get AI-generated listing details (title, description, price, category, condition)."
    )
    @SecurityRequirement(name = "bearerAuth")
    suspend fun analyzeImage(
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<AIListingSuggestion> {
        if (image.isEmpty) throw IllegalArgumentException(IMAGE_EMPTY_ERROR)
        val base64Image = Base64.getEncoder().encodeToString(image.bytes)
        val mimeType = image.contentType ?: "application/octet-stream"
        val suggestion = listingService.analyzeImage("data:$mimeType;base64,$base64Image", mimeType)
        return ResponseEntity.ok(suggestion)
    }

    private fun currentUserId(): UUID =
        SecurityContextHolder.getContext().authentication?.principal as? UUID
            ?: throw IllegalStateException("No authenticated user found")
}
