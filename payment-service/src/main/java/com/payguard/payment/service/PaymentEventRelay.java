package com.payguard.payment.service;

import com.payguard.payment.config.TopicsProperties;
import com.payguard.payment.event.PaymentCreatedEvent;
import com.payguard.payment.event.PaymentReviewedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relays domain events to Kafka only after the originating DB transaction has committed,
 * so a consumer never observes an event for a row it cannot yet read.
 */
@Component
public class PaymentEventRelay {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventRelay.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicsProperties topics;

    public PaymentEventRelay(KafkaTemplate<String, Object> kafkaTemplate, TopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("paymentId={} publishing PaymentCreatedEvent", event.getPaymentId());
        kafkaTemplate.send(topics.getPaymentCreated(), event.getPayerVpa(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentReviewed(PaymentReviewedEvent event) {
        log.info("paymentId={} publishing PaymentReviewedEvent decision={}", event.getPaymentId(), event.getDecision());
        kafkaTemplate.send(topics.getPaymentReviewed(), event.getPayerVpa(), event);
    }
}
