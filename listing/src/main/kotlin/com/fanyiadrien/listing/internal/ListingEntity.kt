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
    var id: UUID? = null,

    @Column(nullable = false)
    var title: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: ListingCategory = ListingCategory.TEXTBOOK,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var condition: ListingCondition = ListingCondition.GOOD,

    @Column(name = "seller_id", nullable = false)
    var sellerId: UUID = UUID(0L, 0L),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ListingStatus = ListingStatus.ACTIVE,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_image_urls", joinColumns = [JoinColumn(name = "listing_id")])
    @Column(name = "image_url")
    var imageUrls: MutableList<String> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

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
