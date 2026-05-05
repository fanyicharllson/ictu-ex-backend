package com.fanyiadrien.shared.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class TokenBlacklistService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    // Use a short, fixed prefix and store by token identifier (jti) to avoid long keys
    private val prefix = "blacklist:jti:"

    // Add jti to blacklist with expiry matching JWT expiry
    fun blacklist(jti: String, expirySeconds: Long) {
        redisTemplate.opsForValue().set(
            "$prefix$jti",
            "blacklisted",
            Duration.ofSeconds(expirySeconds)
        )
    }

    // Check if jti is blacklisted
    fun isBlacklisted(jti: String): Boolean {
        return redisTemplate.hasKey("$prefix$jti") == true
    }
}