package com.fanyiadrien.listing.internal

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
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
        Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ))
    }

    fun uploadImage(imageUrl: String): String {
        return try {
            // imageUrl can be a base64 string (data:image/png;base64,...) or a direct URL
            val uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.emptyMap())
            val secureUrl = uploadResult["secure_url"] as String
            log.debug("Image uploaded to Cloudinary: {}", secureUrl)
            secureUrl
        } catch (e: Exception) {
            log.error("Failed to upload image to Cloudinary: {}", e.message, e)
            throw RuntimeException("Failed to upload image to Cloudinary", e)
        }
    }
}
