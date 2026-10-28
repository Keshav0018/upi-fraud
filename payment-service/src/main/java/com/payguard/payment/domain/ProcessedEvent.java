package com.payguard.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static ProcessedEvent of(UUID eventId, String topic) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.eventId = eventId;
        processedEvent.topic = topic;
        processedEvent.processedAt = Instant.now();
        return processedEvent;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTopic() {
        return topic;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
