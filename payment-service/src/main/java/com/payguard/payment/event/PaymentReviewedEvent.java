package com.payguard.payment.event;

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

    public PaymentReviewedEvent() {
    }

    public PaymentReviewedEvent(UUID eventId, UUID paymentId, Instant timestamp, String decision,
                                  String payerVpa, String payeeVpa, String deviceId) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.decision = decision;
        this.payerVpa = payerVpa;
        this.payeeVpa = payeeVpa;
        this.deviceId = deviceId;
    }

    public static PaymentReviewedEvent of(UUID paymentId, String decision, String payerVpa, String payeeVpa, String deviceId) {
        return new PaymentReviewedEvent(UUID.randomUUID(), paymentId, Instant.now(), decision, payerVpa, payeeVpa, deviceId);
    }

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
