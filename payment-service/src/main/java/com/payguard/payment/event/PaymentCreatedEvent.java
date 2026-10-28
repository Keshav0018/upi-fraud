package com.payguard.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentCreatedEvent {
    private UUID eventId;
    private UUID paymentId;
    private Instant timestamp;
    private String payerVpa;
    private String payeeVpa;
    private BigDecimal amount;
    private String deviceId;

    public PaymentCreatedEvent() {
    }

    public PaymentCreatedEvent(UUID eventId, UUID paymentId, Instant timestamp, String payerVpa,
                                 String payeeVpa, BigDecimal amount, String deviceId) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.payerVpa = payerVpa;
        this.payeeVpa = payeeVpa;
        this.amount = amount;
        this.deviceId = deviceId;
    }

    public static PaymentCreatedEvent of(UUID paymentId, String payerVpa, String payeeVpa,
                                           BigDecimal amount, String deviceId) {
        return new PaymentCreatedEvent(UUID.randomUUID(), paymentId, Instant.now(), payerVpa, payeeVpa, amount, deviceId);
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
