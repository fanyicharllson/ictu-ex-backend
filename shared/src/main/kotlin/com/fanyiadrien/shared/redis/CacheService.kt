package com.fanyiadrien.shared.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CacheService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val listingCacheTtl = Duration.ofMinutes(5)

    fun set(key: String, value: String, ttl: Duration = listingCacheTtl) {
        redisTemplate.opsForValue().set(key, value, ttl)
    }

    fun get(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    fun evict(key: String) {
        redisTemplate.delete(key)
    }

    fun evictByPattern(pattern: String) {
        val keys = redisTemplate.keys(pattern)
        if (keys.isNotEmpty()) redisTemplate.delete(keys)
    }
}