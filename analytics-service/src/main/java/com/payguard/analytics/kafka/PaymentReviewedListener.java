package com.payguard.analytics.kafka;

import com.payguard.analytics.event.PaymentReviewedEvent;
import com.payguard.analytics.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentReviewedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentReviewedListener.class);

    private final ProfileService profileService;
    private final ProcessedEventCache processedEventCache;

    public PaymentReviewedListener(ProfileService profileService, ProcessedEventCache processedEventCache) {
        this.profileService = profileService;
        this.processedEventCache = processedEventCache;
    }

    @KafkaListener(topics = "${payguard.topics.payment-reviewed}", groupId = "analytics-service")
    public void onMessage(PaymentReviewedEvent event) {
        if (!processedEventCache.markIfNew(event.getEventId())) {
            return;
        }
        if ("CLEARED".equals(event.getDecision())) {
            profileService.getOrCreate(event.getPayerVpa()).applyCleared(event.getDeviceId(), event.getPayeeVpa());
            log.info("paymentId={} payer={} device and payee added to known lists after CLEARED review",
                    event.getPaymentId(), event.getPayerVpa());
        }
    }
}
