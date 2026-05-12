package com.fanyiadrien.listing

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/listings")
class ListingController(private val listingService: ListingService) {

    @PostMapping
    fun createListing(@RequestBody request: CreateListingRequest): ResponseEntity<Listing> {
        val listing = listingService.createListing(request, currentUserId())
        return ResponseEntity.status(HttpStatus.CREATED).body(listing)
    }

    @GetMapping
    fun getAllListings(): ResponseEntity<List<Listing>> =
        ResponseEntity.ok(listingService.getAllActiveListings())

    @GetMapping("/{id}")
    fun getListingById(@PathVariable id: UUID): ResponseEntity<Listing> =
        ResponseEntity.ok(listingService.getListingById(id))

    @PutMapping("/{id}")
    fun updateListing(
        @PathVariable id: UUID,
        @RequestBody request: UpdateListingRequest
    ): ResponseEntity<Listing> =
        ResponseEntity.ok(listingService.updateListing(id, request, currentUserId()))

    @DeleteMapping("/{id}")
    fun deleteListing(@PathVariable id: UUID): ResponseEntity<Void> {
        listingService.deleteListing(id, currentUserId())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun searchListings(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<Listing>> =
        ResponseEntity.ok(listingService.searchListings(title, category))

    private fun currentUserId(): UUID =
        SecurityContextHolder.getContext().authentication?.principal as? UUID
            ?: throw IllegalStateException("No authenticated user found")
}
