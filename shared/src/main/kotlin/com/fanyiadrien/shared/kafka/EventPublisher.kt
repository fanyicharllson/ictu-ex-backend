package com.fanyiadrien.shared.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    fun publish(topic: String, event: Any) {
        try {
            val json = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(topic, json)
                .whenComplete { _, ex ->
                    if (ex != null) {
                        logger.error("Failed to publish event to topic $topic", ex)
                    } else {
                        logger.info("✅ Event published to topic: $topic")
                    }
                }
        } catch (e: Exception) {
            logger.error("Error publishing event to topic $topic", e)
        }
    }
}