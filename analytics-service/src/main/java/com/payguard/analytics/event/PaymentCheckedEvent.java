package com.payguard.analytics.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaymentCheckedEvent {
    private UUID eventId;
    private UUID paymentId;
    private Instant timestamp;
    private String result;
    private List<String> reasons;
    private long checkLatencyMs;
    private Map<String, Object> profileSnapshot;

    public PaymentCheckedEvent() {
    }

    public PaymentCheckedEvent(UUID eventId, UUID paymentId, Instant timestamp, String result,
                                 List<String> reasons, long checkLatencyMs, Map<String, Object> profileSnapshot) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.result = result;
        this.reasons = reasons;
        this.checkLatencyMs = checkLatencyMs;
        this.profileSnapshot = profileSnapshot;
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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public long getCheckLatencyMs() {
        return checkLatencyMs;
    }

    public void setCheckLatencyMs(long checkLatencyMs) {
        this.checkLatencyMs = checkLatencyMs;
    }

    public Map<String, Object> getProfileSnapshot() {
        return profileSnapshot;
    }

    public void setProfileSnapshot(Map<String, Object> profileSnapshot) {
        this.profileSnapshot = profileSnapshot;
    }
}
