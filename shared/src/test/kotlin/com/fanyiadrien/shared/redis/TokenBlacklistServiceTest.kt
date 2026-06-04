package com.fanyiadrien.shared.redis

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class TokenBlacklistServiceTest {

    private val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val service = TokenBlacklistService(redisTemplate)

    @Test
    fun `blacklist stores jti key with expected ttl`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)

        service.blacklist("jti-123", 120)

        verify(valueOperations).set(
            eq("blacklist:jti:jti-123"),
            eq("blacklisted"),
            eq(Duration.ofSeconds(120))
        )
    }

    @Test
    fun `isBlacklisted returns true when key exists`() {
        `when`(redisTemplate.hasKey("blacklist:jti:jti-123")).thenReturn(true)

        assertTrue(service.isBlacklisted("jti-123"))
    }

    @Test
    fun `isBlacklisted returns false when key does not exist`() {
        `when`(redisTemplate.hasKey("blacklist:jti:jti-123")).thenReturn(false)

        assertFalse(service.isBlacklisted("jti-123"))
    }
}

