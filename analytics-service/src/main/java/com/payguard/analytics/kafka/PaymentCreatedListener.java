package com.payguard.analytics.kafka;

import com.payguard.analytics.event.PaymentCreatedEvent;
import com.payguard.analytics.service.CheckService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedListener {

    private final CheckService checkService;
    private final ProcessedEventCache processedEventCache;

    public PaymentCreatedListener(CheckService checkService, ProcessedEventCache processedEventCache) {
        this.checkService = checkService;
        this.processedEventCache = processedEventCache;
    }

    @KafkaListener(topics = "${payguard.topics.payment-created}", groupId = "analytics-service")
    public void onMessage(PaymentCreatedEvent event) {
        if (!processedEventCache.markIfNew(event.getEventId())) {
            return;
        }
        checkService.handle(event);
    }
}
