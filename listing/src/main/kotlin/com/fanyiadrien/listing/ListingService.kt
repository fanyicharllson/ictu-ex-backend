package com.fanyiadrien.listing

import java.math.BigDecimal
import java.util.UUID

interface ListingService {
    fun createListing(request: CreateListingRequest, sellerId: UUID): Listing
    fun getAllActiveListings(): List<Listing>
    fun getListingById(id: UUID): Listing
    fun updateListing(id: UUID, request: UpdateListingRequest, sellerId: UUID): Listing
    fun deleteListing(id: UUID, sellerId: UUID)
    fun searchListings(title: String?, category: String?): List<Listing>
}

data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: BigDecimal,
    val category: String,
    val condition: String,
    val imageUrls: List<String> = emptyList()
)

data class UpdateListingRequest(
    val title: String?,
    val description: String?,
    val price: BigDecimal?,
    val category: String?,
    val condition: String?,
    val status: String?,
    val imageUrls: List<String>?
)
