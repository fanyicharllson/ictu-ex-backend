package com.fanyiadrien.listing

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Public read model exposed by the listing module.
 * Other modules depend on this — never on internal classes.
 */
data class Listing(
    val id: UUID,
    val title: String,
    val description: String,
    val price: BigDecimal,
    val category: String,
    val condition: String,
    val sellerId: UUID,
    val status: String,
    val imageUrls: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
