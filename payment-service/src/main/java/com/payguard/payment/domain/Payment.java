package com.payguard.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false)
    private String payeeVpa;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 280)
    private String note;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "check_latency_ms")
    private Long checkLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Payment newPending(String payerVpa, String payeeVpa, BigDecimal amount, String note, String deviceId) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.payerVpa = payerVpa;
        payment.payeeVpa = payeeVpa;
        payment.amount = amount;
        payment.note = note;
        payment.deviceId = deviceId;
        payment.status = PaymentStatus.PENDING_CHECK;
        Instant now = Instant.now();
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public void touch() {
        this.updatedAt = Instant.now();
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

    public void setNote(String note) {
        this.note = note;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Long getCheckLatencyMs() {
        return checkLatencyMs;
    }

    public void setCheckLatencyMs(Long checkLatencyMs) {
        this.checkLatencyMs = checkLatencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
