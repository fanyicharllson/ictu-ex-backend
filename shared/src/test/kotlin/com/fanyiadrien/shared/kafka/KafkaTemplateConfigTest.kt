package com.fanyiadrien.shared.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory

class KafkaTemplateConfigTest {

    @Test
    fun `producerFactory contains required kafka producer properties`() {
        val config = KafkaTemplateConfig("localhost:9092")

        val producerFactory = config.producerFactory() as DefaultKafkaProducerFactory<String, String>

        assertEquals(
            "localhost:9092",
            producerFactory.configurationProperties[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG]
        )
        assertEquals(
            StringSerializer::class.java,
            producerFactory.configurationProperties[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG]
        )
        assertEquals(
            StringSerializer::class.java,
            producerFactory.configurationProperties[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG]
        )
    }

    @Test
    fun `kafkaTemplate and kotlinModule beans are created`() {
        val config = KafkaTemplateConfig("localhost:9092")

        val kafkaTemplate = config.kafkaTemplate()
        val kotlinModule = config.kotlinModule()

        assertNotNull(kafkaTemplate)
        assertNotNull(kotlinModule)
    }
}

