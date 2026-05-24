package com.fanyiadrien.listing

data class AIListingSuggestion(
    val title: String,
    val description: String,
    val suggestedPrice: Double,
    val category: String,
    val condition: String
)
