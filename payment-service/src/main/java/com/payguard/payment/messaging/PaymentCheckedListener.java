package com.payguard.payment.messaging;

import com.payguard.payment.event.PaymentCheckedEvent;
import com.payguard.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCheckedListener {

    private final PaymentService paymentService;

    public PaymentCheckedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${payguard.topics.payment-checked}", groupId = "payment-service")
    public void onMessage(PaymentCheckedEvent event) {
        paymentService.applyCheckedResult(event);
    }
}
