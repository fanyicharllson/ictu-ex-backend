package com.fanyiadrien.shared.events

import java.time.Instant
import java.util.UUID

data class ProductPostedEvent(
    val listingId: UUID,
    val sellerId: UUID,
    val title: String,
    val category: String,
    val occurredAt: Instant = Instant.now()
)
