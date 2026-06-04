package com.fanyiadrien.shared.redis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class CacheServiceTest {

    private val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val service = CacheService(redisTemplate)

    @Test
    fun `set writes key value and ttl to redis`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        val ttl = Duration.ofMinutes(10)

        service.set("listing:1", "payload", ttl)

        verify(valueOperations).set(eq("listing:1"), eq("payload"), eq(ttl))
    }

    @Test
    fun `get reads value from redis`() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get("listing:1")).thenReturn("cached")

        val value = service.get("listing:1")

        assertEquals("cached", value)
    }

    @Test
    fun `evict removes a single key`() {
        service.evict("listing:1")

        verify(redisTemplate).delete("listing:1")
    }

    @Test
    fun `evictByPattern deletes all matching keys when found`() {
        val keys = mutableSetOf("listings:1", "listings:2")
        `when`(redisTemplate.keys("listings:*")).thenReturn(keys)

        service.evictByPattern("listings:*")

        verify(redisTemplate).delete(keys as Collection<String>)
    }

    @Test
    fun `evictByPattern does nothing when no keys match`() {
        `when`(redisTemplate.keys("listings:*")).thenReturn(mutableSetOf())

        service.evictByPattern("listings:*")

        verify(redisTemplate, never()).delete(anyCollection())
    }
}


