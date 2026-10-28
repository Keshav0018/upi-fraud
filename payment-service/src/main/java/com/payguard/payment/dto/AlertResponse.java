package com.payguard.payment.dto;

import com.payguard.payment.domain.AiStatus;
import com.payguard.payment.domain.AlertStatus;
import com.payguard.payment.domain.FraudAlert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AlertResponse {
    private final UUID id;
    private final UUID paymentId;
    private final String payerVpa;
    private final String payeeVpa;
    private final BigDecimal amount;
    private final List<String> reasons;
    private final Map<String, Object> profileSnapshot;
    private final AlertStatus status;
    private final AiStatus aiStatus;
    private final String aiRiskLevel;
    private final String aiSummary;
    private final String aiRecommendation;
    private final List<String> aiSignals;
    private final Instant createdAt;
    private final Instant reviewedAt;

    public AlertResponse(UUID id, UUID paymentId, String payerVpa, String payeeVpa, BigDecimal amount,
                          List<String> reasons, Map<String, Object> profileSnapshot, AlertStatus status,
                          AiStatus aiStatus, String aiRiskLevel, String aiSummary, String aiRecommendation,
                          List<String> aiSignals, Instant createdAt, Instant reviewedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.payerVpa = payerVpa;
        this.payeeVpa = payeeVpa;
        this.amount = amount;
        this.reasons = reasons;
        this.profileSnapshot = profileSnapshot;
        this.status = status;
        this.aiStatus = aiStatus;
        this.aiRiskLevel = aiRiskLevel;
        this.aiSummary = aiSummary;
        this.aiRecommendation = aiRecommendation;
        this.aiSignals = aiSignals;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public static AlertResponse from(FraudAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getPaymentId(),
                alert.getPayerVpa(),
                alert.getPayeeVpa(),
                alert.getAmount(),
                alert.getReasons(),
                alert.getProfileSnapshot(),
                alert.getStatus(),
                alert.getAiStatus(),
                alert.getAiRiskLevel(),
                alert.getAiSummary(),
                alert.getAiRecommendation(),
                alert.getAiSignals(),
                alert.getCreatedAt(),
                alert.getReviewedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
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

    public List<String> getReasons() {
        return reasons;
    }

    public Map<String, Object> getProfileSnapshot() {
        return profileSnapshot;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public AiStatus getAiStatus() {
        return aiStatus;
    }

    public String getAiRiskLevel() {
        return aiRiskLevel;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public List<String> getAiSignals() {
        return aiSignals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
