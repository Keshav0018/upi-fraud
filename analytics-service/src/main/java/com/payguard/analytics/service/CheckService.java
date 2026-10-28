package com.payguard.analytics.service;

import com.payguard.analytics.ai.FraudAnalysisService;
import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCheckedEvent;
import com.payguard.analytics.event.PaymentCreatedEvent;
import com.payguard.analytics.kafka.EventPublisher;
import com.payguard.analytics.rules.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the fraud rules synchronously (fast, in-memory, never touches the network) and publishes
 * the verdict immediately. The LLM explanation for a HELD payment happens afterwards, asynchronously.
 */
@Service
public class CheckService {

    private static final Logger log = LoggerFactory.getLogger(CheckService.class);

    private final ProfileService profileService;
    private final RuleEngine ruleEngine;
    private final EventPublisher eventPublisher;
    private final HeldPaymentsHistory heldPaymentsHistory;
    private final FraudAnalysisService fraudAnalysisService;

    public CheckService(ProfileService profileService, RuleEngine ruleEngine, EventPublisher eventPublisher,
                         HeldPaymentsHistory heldPaymentsHistory, FraudAnalysisService fraudAnalysisService) {
        this.profileService = profileService;
        this.ruleEngine = ruleEngine;
        this.eventPublisher = eventPublisher;
        this.heldPaymentsHistory = heldPaymentsHistory;
        this.fraudAnalysisService = fraudAnalysisService;
    }

    public void handle(PaymentCreatedEvent event) {
        long start = System.nanoTime();

        PayerProfile profile = profileService.getOrCreate(event.getPayerVpa());
        Instant attemptTime = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
        int recentCount = profile.recordAttemptAndCountRecent(attemptTime);

        List<String> reasons = ruleEngine.evaluate(event, profile, recentCount);
        boolean held = !reasons.isEmpty();

        PayerProfile.Snapshot snapshot = profile.snapshot(recentCount);
        Map<String, Object> profileSnapshot = new LinkedHashMap<>();
        profileSnapshot.put("averageAmount", snapshot.averageAmount());
        profileSnapshot.put("knownDeviceCount", snapshot.knownDeviceCount());
        profileSnapshot.put("knownPayeeCount", snapshot.knownPayeeCount());
        profileSnapshot.put("recentPaymentCount", snapshot.recentPaymentCount());

        // A device is "known" once we've seen a payer use it at all, regardless of outcome —
        // otherwise a payer flagged on their unavoidably-cold first payment could never produce
        // an approved one afterwards, since approval itself requires something to already be known.
        profile.recordDeviceSeen(event.getDeviceId());
        if (!held) {
            profile.applyApproved(event.getAmount().doubleValue(), event.getPayeeVpa());
        }

        long latencyMs = Math.max(0, (System.nanoTime() - start) / 1_000_000);

        PaymentCheckedEvent checkedEvent = new PaymentCheckedEvent(UUID.randomUUID(), event.getPaymentId(),
                Instant.now(), held ? "HELD" : "APPROVED", reasons, latencyMs, profileSnapshot);
        eventPublisher.publishChecked(event.getPayerVpa(), checkedEvent);

        log.info("paymentId={} result={} reasons={} latencyMs={}", event.getPaymentId(),
                checkedEvent.getResult(), reasons, latencyMs);

        if (held) {
            heldPaymentsHistory.record(event, reasons);
            fraudAnalysisService.analyzeAsync(event, reasons, profileSnapshot);
        }
    }
}
