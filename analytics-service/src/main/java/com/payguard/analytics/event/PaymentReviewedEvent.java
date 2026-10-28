package com.payguard.analytics.event;

import java.time.Instant;
import java.util.UUID;

public class PaymentReviewedEvent {
    private UUID eventId;
    private UUID paymentId;
    private Instant timestamp;
    private String decision;
    private String payerVpa;
    private String payeeVpa;
    private String deviceId;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getPayerVpa() {
        return payerVpa;
    }

    public void setPayerVpa(String payerVpa) {
        this.payerVpa = payerVpa;
    }

    public String getPayeeVpa() {
        return payeeVpa;
    }

    public void setPayeeVpa(String payeeVpa) {
        this.payeeVpa = payeeVpa;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
