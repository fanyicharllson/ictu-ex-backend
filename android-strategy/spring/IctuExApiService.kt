package com.fanyiadrien.ictu_ex.data.remote.spring

import com.fanyiadrien.ictu_ex.data.remote.api.AuthResult
import com.fanyiadrien.ictu_ex.data.remote.api.AuthUser
import com.fanyiadrien.ictu_ex.data.remote.api.Conversation
import com.fanyiadrien.ictu_ex.data.remote.api.Listing
import com.fanyiadrien.ictu_ex.data.remote.api.Message
import retrofit2.http.*

/**
 * STEP 3 — RETROFIT API INTERFACE
 *
 * Maps 1-to-1 with the Spring Boot REST endpoints.
 * Base URL: https://api.ictuex.teamnest.me
 */

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val studentId: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdateUserTypeRequest(val userType: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ResendTokenRequest(val email: String)
data class MessageResponse(val message: String)

// ─── Listing DTOs ─────────────────────────────────────────────────────────────

data class SpringCreateListingRequest(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String,
    val imageUrls: List<String>
)

data class SpringUpdateListingRequest(
    val title: String?,
    val description: String?,
    val price: Double?,
    val category: String?,
    val condition: String?,
    val status: String?,
    val imageUrls: List<String>?
)

// ─── Messaging DTOs ───────────────────────────────────────────────────────────

data class CreateConversationRequest(
    val otherUserId: String,
    val listingId: String? = null
)

data class SendMessageRequest(val content: String)

// ─── Retrofit Interface ───────────────────────────────────────────────────────

interface IctuExApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResult

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResult

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): MessageResponse

    @GET("api/auth/validate")
    suspend fun validateToken(@Header("Authorization") token: String): AuthUser

    @PATCH("api/auth/user-type")
    suspend fun updateUserType(
        @Header("Authorization") token: String,
        @Body request: UpdateUserTypeRequest
    ): AuthUser

    @POST("api/auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): MessageResponse

    @POST("api/auth/resend-token")
    suspend fun resendVerificationCode(@Body request: ResendTokenRequest): MessageResponse

    // Listings
    @GET("api/listings")
    suspend fun getAllListings(): List<Listing>

    @GET("api/listings/{id}")
    suspend fun getListingById(@Path("id") id: String): Listing

    @GET("api/listings/search")
    suspend fun searchListings(
        @Query("title") title: String?,
        @Query("category") category: String?
    ): List<Listing>

    @POST("api/listings")
    suspend fun createListing(
        @Header("Authorization") token: String,
        @Body request: SpringCreateListingRequest
    ): Listing

    @PUT("api/listings/{id}")
    suspend fun updateListing(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: SpringUpdateListingRequest
    ): Listing

    @DELETE("api/listings/{id}")
    suspend fun deleteListing(
        @Header("Authorization") token: String,
        @Path("id") id: String
    )

    // Messaging
    @POST("api/messaging/conversations")
    suspend fun getOrCreateConversation(
        @Header("Authorization") token: String,
        @Body request: CreateConversationRequest
    ): Conversation

    @POST("api/messaging/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Message

    @GET("api/messaging/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String
    ): List<Message>

    @GET("api/messaging/conversations")
    suspend fun getMyConversations(@Header("Authorization") token: String): List<Conversation>
}
