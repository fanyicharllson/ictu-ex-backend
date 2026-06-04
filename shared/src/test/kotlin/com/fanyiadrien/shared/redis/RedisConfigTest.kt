package com.fanyiadrien.shared.redis

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.StringRedisSerializer

class RedisConfigTest {

    private val config = RedisConfig()

    @Test
    fun `redisTemplate wires connection factory and string serializers`() {
        val factory = mock(RedisConnectionFactory::class.java)

        val template = config.redisTemplate(factory)

        assertSame(factory, template.connectionFactory)
        assertTrue(template.keySerializer is StringRedisSerializer)
        assertTrue(template.valueSerializer is StringRedisSerializer)
        assertTrue(template.hashKeySerializer is StringRedisSerializer)
        assertTrue(template.hashValueSerializer is StringRedisSerializer)
        assertNotNull(template)
    }
}

