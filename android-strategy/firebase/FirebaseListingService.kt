package com.fanyiadrien.ictu_ex.data.remote.firebase

import com.fanyiadrien.ictu_ex.data.remote.api.CreateListingRequest
import com.fanyiadrien.ictu_ex.data.remote.api.Listing
import com.fanyiadrien.ictu_ex.data.remote.api.ListingService
import com.fanyiadrien.ictu_ex.data.remote.api.UpdateListingRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import javax.inject.Inject

/**
 * STEP 2 — FIREBASE LISTING IMPLEMENTATION
 *
 * Reads/writes listings from Firestore collection "listings".
 * Mirrors the same fields your Spring Boot backend uses.
 */
class FirebaseListingService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ListingService {

    private val collection get() = firestore.collection("listings")

    override suspend fun getAllListings(): Result<List<Listing>> = runCatching {
        collection.whereEqualTo("status", "ACTIVE").get().await()
            .documents.mapNotNull { it.toListing() }
    }

    override suspend fun getListingById(id: String): Result<Listing> = runCatching {
        collection.document(id).get().await().toListing()
            ?: throw NoSuchElementException("Listing not found: $id")
    }

    override suspend fun searchListings(title: String?, category: String?): Result<List<Listing>> = runCatching {
        var query = collection.whereEqualTo("status", "ACTIVE")
        if (!category.isNullOrBlank()) query = query.whereEqualTo("category", category)
        val results = query.get().await().documents.mapNotNull { it.toListing() }
        if (!title.isNullOrBlank()) results.filter { it.title.contains(title, ignoreCase = true) }
        else results
    }

    override suspend fun createListing(request: CreateListingRequest): Result<Listing> = runCatching {
        val sellerId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Not authenticated")
        val now = System.currentTimeMillis().toString()
        val doc = collection.document()
        val data = mapOf(
            "id"          to doc.id,
            "title"       to request.title,
            "description" to request.description,
            "price"       to request.price.toDouble(),
            "category"    to request.category,
            "condition"   to request.condition,
            "sellerId"    to sellerId,
            "status"      to "ACTIVE",
            "imageUrls"   to request.imageUrls,
            "createdAt"   to now,
            "updatedAt"   to now
        )
        doc.set(data).await()
        Listing(
            id = doc.id, title = request.title, description = request.description,
            price = request.price, category = request.category, condition = request.condition,
            sellerId = sellerId, status = "ACTIVE", imageUrls = request.imageUrls,
            createdAt = now, updatedAt = now
        )
    }

    override suspend fun updateListing(id: String, request: UpdateListingRequest): Result<Listing> = runCatching {
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis().toString())
        request.title?.let       { updates["title"] = it }
        request.description?.let { updates["description"] = it }
        request.price?.let       { updates["price"] = it.toDouble() }
        request.category?.let    { updates["category"] = it }
        request.condition?.let   { updates["condition"] = it }
        request.status?.let      { updates["status"] = it }
        request.imageUrls?.let   { updates["imageUrls"] = it }
        collection.document(id).update(updates).await()
        getListingById(id).getOrThrow()
    }

    override suspend fun deleteListing(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toListing(): Listing? {
        if (!exists()) return null
        return Listing(
            id          = id,
            title       = getString("title") ?: "",
            description = getString("description") ?: "",
            price       = BigDecimal.valueOf(getDouble("price") ?: 0.0),
            category    = getString("category") ?: "",
            condition   = getString("condition") ?: "",
            sellerId    = getString("sellerId") ?: "",
            status      = getString("status") ?: "ACTIVE",
            imageUrls   = get("imageUrls") as? List<String> ?: emptyList(),
            createdAt   = getString("createdAt") ?: "",
            updatedAt   = getString("updatedAt") ?: ""
        )
    }
}
