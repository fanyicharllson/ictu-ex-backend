package com.fanyiadrien.shared.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class EventPublisherTest {

    private val kafkaTemplate = mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
    private val objectMapper = mock(ObjectMapper::class.java)
    private val publisher = EventPublisher(kafkaTemplate, objectMapper)

    @Test
    fun `publish sends serialized payload to kafka`() {
        val event = mapOf("type" to "user.registered")
        val future = CompletableFuture<SendResult<String, String>>()

        `when`(objectMapper.writeValueAsString(event)).thenReturn("{\"type\":\"user.registered\"}")
        `when`(kafkaTemplate.send(eq("topic.events"), eq("{\"type\":\"user.registered\"}"))).thenReturn(future)

        publisher.publish("topic.events", event)
        future.complete(mock(SendResult::class.java) as SendResult<String, String>)

        verify(objectMapper).writeValueAsString(event)
        verify(kafkaTemplate).send("topic.events", "{\"type\":\"user.registered\"}")
    }

    @Test
    fun `publish swallows serialization exception and does not send`() {
        val event = mapOf("type" to "bad")
        `when`(objectMapper.writeValueAsString(event)).thenThrow(RuntimeException("serialization error"))

        publisher.publish("topic.events", event)

        verify(kafkaTemplate, never()).send(any(), any())
    }
}

