package com.fanyiadrien.shared.events

import java.util.UUID

data class ImageUploadEvent(
    val listingId: UUID,
    val originalImageUrl: String,
    val imageIndex: Int // To know which image URL to replace in the list
)
