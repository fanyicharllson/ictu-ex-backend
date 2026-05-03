package com.fanyiadrien.shared.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Bean
    fun userRegisteredTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.USER_REGISTERED)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun verificationCodeTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.VERIFICATION_CODE_GENERATED)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun productPostedTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.PRODUCT_POSTED)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun messageSentTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.MESSAGE_SENT)
            .partitions(1)
            .replicas(1)
            .build()
}
