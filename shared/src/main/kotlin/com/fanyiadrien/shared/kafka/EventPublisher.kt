package com.fanyiadrien.shared.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    fun publish(topic: String, event: Any) {
        val json = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(topic, json)
    }
}