package com.payguard.analytics.service;

import com.payguard.analytics.event.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Keeps the last hour's HELD payments in memory, for the "AI summary of the last hour" feature. */
@Component
public class HeldPaymentsHistory {

    private static final Duration WINDOW = Duration.ofHours(1);

    public record Entry(Instant timestamp, String payerVpa, String payeeVpa, BigDecimal amount, List<String> reasons) {
    }

    private final ConcurrentLinkedDeque<Entry> entries = new ConcurrentLinkedDeque<>();

    public void record(PaymentCreatedEvent event, List<String> reasons) {
        entries.addLast(new Entry(Instant.now(), event.getPayerVpa(), event.getPayeeVpa(), event.getAmount(), reasons));
        prune();
    }

    public List<Entry> recentEntries() {
        prune();
        return List.copyOf(entries);
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (true) {
            Entry oldest = entries.peekFirst();
            if (oldest == null || !oldest.timestamp().isBefore(cutoff)) {
                break;
            }
            entries.pollFirst();
        }
    }
}
