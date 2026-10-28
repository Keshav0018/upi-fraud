package com.payguard.payment.messaging;

import com.payguard.payment.event.PaymentAnalyzedEvent;
import com.payguard.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentAnalyzedListener {

    private final PaymentService paymentService;

    public PaymentAnalyzedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${payguard.topics.payment-analyzed}", groupId = "payment-service")
    public void onMessage(PaymentAnalyzedEvent event) {
        paymentService.applyAnalyzedResult(event);
    }
}
