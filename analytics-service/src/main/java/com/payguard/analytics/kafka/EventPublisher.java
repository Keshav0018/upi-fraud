package com.payguard.analytics.kafka;

import com.payguard.analytics.config.TopicsProperties;
import com.payguard.analytics.event.PaymentAnalyzedEvent;
import com.payguard.analytics.event.PaymentCheckedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicsProperties topics;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate, TopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void publishChecked(String payerVpa, PaymentCheckedEvent event) {
        kafkaTemplate.send(topics.getPaymentChecked(), payerVpa, event);
    }

    public void publishAnalyzed(String payerVpa, PaymentAnalyzedEvent event) {
        kafkaTemplate.send(topics.getPaymentAnalyzed(), payerVpa, event);
    }
}
