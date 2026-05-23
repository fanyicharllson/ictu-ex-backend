package com.fanyiadrien.ictu_ex.data.remote.spring

import com.fanyiadrien.ictu_ex.data.remote.api.CreateListingRequest
import com.fanyiadrien.ictu_ex.data.remote.api.Listing
import com.fanyiadrien.ictu_ex.data.remote.api.ListingService
import com.fanyiadrien.ictu_ex.data.remote.api.UpdateListingRequest
import javax.inject.Inject

/**
 * STEP 3 — SPRING BOOT LISTING IMPLEMENTATION
 */
class SpringListingService @Inject constructor(
    private val api: IctuExApiService,
    private val tokenStore: TokenStore
) : ListingService {

    override suspend fun getAllListings(): Result<List<Listing>> = runCatching {
        api.getAllListings()
    }

    override suspend fun getListingById(id: String): Result<Listing> = runCatching {
        api.getListingById(id)
    }

    override suspend fun searchListings(title: String?, category: String?): Result<List<Listing>> = runCatching {
        api.searchListings(title, category)
    }

    override suspend fun createListing(request: CreateListingRequest): Result<Listing> = runCatching {
        val bearer = tokenStore.getBearerToken()
            ?: throw IllegalStateException("Not authenticated")
        api.createListing(
            token = bearer,
            request = SpringCreateListingRequest(
                title       = request.title,
                description = request.description,
                price       = request.price.toDouble(),
                category    = request.category,
                condition   = request.condition,
                imageUrls   = request.imageUrls
            )
        )
    }

    override suspend fun updateListing(id: String, request: UpdateListingRequest): Result<Listing> = runCatching {
        val bearer = tokenStore.getBearerToken()
            ?: throw IllegalStateException("Not authenticated")
        api.updateListing(
            token = bearer,
            id    = id,
            request = SpringUpdateListingRequest(
                title       = request.title,
                description = request.description,
                price       = request.price?.toDouble(),
                category    = request.category,
                condition   = request.condition,
                status      = request.status,
                imageUrls   = request.imageUrls
            )
        )
    }

    override suspend fun deleteListing(id: String): Result<Unit> = runCatching {
        val bearer = tokenStore.getBearerToken()
            ?: throw IllegalStateException("Not authenticated")
        api.deleteListing(bearer, id)
    }
}
