package com.fanyiadrien.ictu_ex.data.remote.api

import java.math.BigDecimal

// ─── Models ──────────────────────────────────────────────────────────────────

data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val price: BigDecimal,
    val category: String,   // TEXTBOOK | ELECTRONICS | HOSTEL_GEAR | CLOTHING | STATIONERY | FOOD | SPORTS | OTHER
    val condition: String,  // NEW | GOOD | FAIR
    val sellerId: String,
    val status: String,     // ACTIVE | SOLD | SWAPPED
    val imageUrls: List<String>,
    val createdAt: String,
    val updatedAt: String
)

data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: BigDecimal,
    val category: String,
    val condition: String,
    val imageUrls: List<String> = emptyList()
)

data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val category: String? = null,
    val condition: String? = null,
    val status: String? = null,
    val imageUrls: List<String>? = null
)

// ─── Interface ───────────────────────────────────────────────────────────────

interface ListingService {
    /** Browse all active listings. No auth required. */
    suspend fun getAllListings(): Result<List<Listing>>

    /** Get a single listing by its ID. No auth required. */
    suspend fun getListingById(id: String): Result<Listing>

    /** Search listings by title and/or category. No auth required. */
    suspend fun searchListings(title: String? = null, category: String? = null): Result<List<Listing>>

    /** Create a new listing. Requires SELLER role. */
    suspend fun createListing(request: CreateListingRequest): Result<Listing>

    /** Update an existing listing. Seller only. */
    suspend fun updateListing(id: String, request: UpdateListingRequest): Result<Listing>

    /** Delete a listing. Seller only. */
    suspend fun deleteListing(id: String): Result<Unit>
}
