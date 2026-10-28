package com.payguard.payment.dto;

import com.payguard.payment.domain.Payment;
import com.payguard.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {
    private final UUID id;
    private final String payerVpa;
    private final String payeeVpa;
    private final BigDecimal amount;
    private final String note;
    private final String deviceId;
    private final PaymentStatus status;
    private final Long checkLatencyMs;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PaymentResponse(UUID id, String payerVpa, String payeeVpa, BigDecimal amount, String note,
                            String deviceId, PaymentStatus status, Long checkLatencyMs,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.payerVpa = payerVpa;
        this.payeeVpa = payeeVpa;
        this.amount = amount;
        this.note = note;
        this.deviceId = deviceId;
        this.status = status;
        this.checkLatencyMs = checkLatencyMs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPayerVpa(),
                payment.getPayeeVpa(),
                payment.getAmount(),
                payment.getNote(),
                payment.getDeviceId(),
                payment.getStatus(),
                payment.getCheckLatencyMs(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getPayerVpa() {
        return payerVpa;
    }

    public String getPayeeVpa() {
        return payeeVpa;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getCheckLatencyMs() {
        return checkLatencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
