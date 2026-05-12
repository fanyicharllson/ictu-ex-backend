package com.fanyiadrien.listing.internal

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "listings")
internal class ListingEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: ListingCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val condition: ListingCondition,

    @Column(name = "seller_id", nullable = false)
    val sellerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ListingStatus = ListingStatus.ACTIVE,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_image_urls", joinColumns = [JoinColumn(name = "listing_id")])
    @Column(name = "image_url")
    val imageUrls: MutableList<String> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

internal enum class ListingCategory {
    TEXTBOOK, ELECTRONICS, HOSTEL_GEAR, CLOTHING, STATIONERY, FOOD, SPORTS, OTHER
}

internal enum class ListingCondition {
    NEW, GOOD, FAIR
}

internal enum class ListingStatus {
    ACTIVE, SOLD, SWAPPED
}
