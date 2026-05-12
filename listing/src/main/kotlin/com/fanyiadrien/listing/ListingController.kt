package com.fanyiadrien.listing

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/listings")
@Tag(name = "Listings", description = "Marketplace listing endpoints")
class ListingController(private val listingService: ListingService) {

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
    fun deleteListing(@PathVariable id: UUID): ResponseEntity<Void> {
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

    private fun currentUserId(): UUID =
        SecurityContextHolder.getContext().authentication?.principal as? UUID
            ?: throw IllegalStateException("No authenticated user found")
}
