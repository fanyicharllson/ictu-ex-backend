package com.fanyiadrien.listing.internal

import com.cloudinary.Cloudinary
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class CloudinaryService(
    @Value("\${CLOUDINARY_CLOUD_NAME}") private val cloudName: String,
    @Value("\${CLOUDINARY_API_KEY}") private val apiKey: String,
    @Value("\${CLOUDINARY_API_SECRET}") private val apiSecret: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val cloudinary: Cloudinary by lazy {
        Cloudinary(
            mapOf(
                "cloud_name" to cloudName,
                "api_key" to apiKey,
                "api_secret" to apiSecret
            )
        )
    }

    fun uploadImage(imageUrl: String): String {
        return try {
            val uploadResult = cloudinary.uploader().upload(imageUrl, emptyMap<String, Any>())
            val secureUrl = uploadResult["secure_url"] as String
            log.debug("Image uploaded to Cloudinary: {}", secureUrl)
            secureUrl
        } catch (e: Exception) {
            log.error("Failed to upload image to Cloudinary: {}", e.message, e)
            throw RuntimeException("Failed to upload image to Cloudinary", e)
        }
    }
}
