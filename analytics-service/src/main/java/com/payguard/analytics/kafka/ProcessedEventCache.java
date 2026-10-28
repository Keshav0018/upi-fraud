package com.payguard.analytics.kafka;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded in-memory record of already-applied event ids, so a redelivered Kafka message
 * (broker retry, consumer restart before commit) is a no-op instead of double-applying.
 * Oldest ids fall off once the cache is full; this is a demo-scale safeguard, not a durable log.
 */
@Component
public class ProcessedEventCache {

    private static final int MAX_SIZE = 50_000;

    private final Set<UUID> seen = Collections.newSetFromMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                    return size() > MAX_SIZE;
                }
            });

    /** Returns true if this event id had not been seen before (caller should process it). */
    public synchronized boolean markIfNew(UUID eventId) {
        return seen.add(eventId);
    }
}
