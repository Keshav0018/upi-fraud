package com.payguard.analytics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(TopicsProperties.class)
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentCreatedTopic(TopicsProperties topics) {
        return TopicBuilder.name(topics.getPaymentCreated()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCheckedTopic(TopicsProperties topics) {
        return TopicBuilder.name(topics.getPaymentChecked()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAnalyzedTopic(TopicsProperties topics) {
        return TopicBuilder.name(topics.getPaymentAnalyzed()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentReviewedTopic(TopicsProperties topics) {
        return TopicBuilder.name(topics.getPaymentReviewed()).partitions(3).replicas(1).build();
    }
}
