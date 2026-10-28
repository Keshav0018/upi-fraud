package com.payguard.analytics.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PaymentAnalyzedEvent {
    private UUID eventId;
    private UUID paymentId;
    private Instant timestamp;
    private String aiStatus;
    private String riskLevel;
    private String summary;
    private String recommendation;
    private List<String> signals;

    public PaymentAnalyzedEvent() {
    }

    public PaymentAnalyzedEvent(UUID eventId, UUID paymentId, Instant timestamp, String aiStatus,
                                  String riskLevel, String summary, String recommendation, List<String> signals) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.aiStatus = aiStatus;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.recommendation = recommendation;
        this.signals = signals;
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

    public String getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(String aiStatus) {
        this.aiStatus = aiStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }
}
