package com.fanyiadrien.listing.internal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface ListingRepository : JpaRepository<ListingEntity, UUID> {
    fun findByStatus(status: ListingStatus): List<ListingEntity>
    fun findBySellerIdAndStatus(sellerId: UUID, status: ListingStatus): List<ListingEntity>
    fun findByTitleContainingIgnoreCaseAndStatus(title: String, status: ListingStatus): List<ListingEntity>
    fun findByCategoryAndStatus(category: ListingCategory, status: ListingStatus): List<ListingEntity>
}
