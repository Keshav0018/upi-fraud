package com.payguard.payment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false)
    private String payeeVpa;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> reasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_snapshot")
    private Map<String, Object> profileSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false, length = 20)
    private AiStatus aiStatus;

    @Column(name = "ai_risk_level", length = 20)
    private String aiRiskLevel;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_recommendation", length = 20)
    private String aiRecommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_signals")
    private List<String> aiSignals;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public static FraudAlert open(UUID paymentId, String payerVpa, String payeeVpa, BigDecimal amount,
                                    List<String> reasons, Map<String, Object> profileSnapshot) {
        FraudAlert alert = new FraudAlert();
        alert.id = UUID.randomUUID();
        alert.paymentId = paymentId;
        alert.payerVpa = payerVpa;
        alert.payeeVpa = payeeVpa;
        alert.amount = amount;
        alert.reasons = reasons;
        alert.profileSnapshot = profileSnapshot;
        alert.status = AlertStatus.PENDING;
        alert.aiStatus = AiStatus.PENDING;
        alert.createdAt = Instant.now();
        return alert;
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

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public AiStatus getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(AiStatus aiStatus) {
        this.aiStatus = aiStatus;
    }

    public String getAiRiskLevel() {
        return aiRiskLevel;
    }

    public void setAiRiskLevel(String aiRiskLevel) {
        this.aiRiskLevel = aiRiskLevel;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public List<String> getAiSignals() {
        return aiSignals;
    }

    public void setAiSignals(List<String> aiSignals) {
        this.aiSignals = aiSignals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
