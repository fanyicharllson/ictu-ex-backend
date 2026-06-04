package com.fanyiadrien.shared.kafka

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KafkaConfigTest {

    private val config = KafkaConfig()

    @Test
    fun `user registered topic is configured correctly`() {
        val topic = config.userRegisteredTopic()

        assertEquals(KafkaTopics.USER_REGISTERED, topic.name())
        assertEquals(1, topic.numPartitions())
        assertEquals(1.toShort(), topic.replicationFactor())
    }

    @Test
    fun `verification code topic is configured correctly`() {
        val topic = config.verificationCodeTopic()

        assertEquals(KafkaTopics.VERIFICATION_CODE_GENERATED, topic.name())
        assertEquals(1, topic.numPartitions())
        assertEquals(1.toShort(), topic.replicationFactor())
    }

    @Test
    fun `product posted topic is configured correctly`() {
        val topic = config.productPostedTopic()

        assertEquals(KafkaTopics.PRODUCT_POSTED, topic.name())
        assertEquals(1, topic.numPartitions())
        assertEquals(1.toShort(), topic.replicationFactor())
    }

    @Test
    fun `message sent topic is configured correctly`() {
        val topic = config.messageSentTopic()

        assertEquals(KafkaTopics.MESSAGE_SENT, topic.name())
        assertEquals(1, topic.numPartitions())
        assertEquals(1.toShort(), topic.replicationFactor())
    }
}

