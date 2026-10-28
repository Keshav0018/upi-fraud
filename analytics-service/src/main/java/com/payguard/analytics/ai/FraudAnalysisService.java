package com.payguard.analytics.ai;

import com.payguard.analytics.config.AiProperties;
import com.payguard.analytics.event.PaymentAnalyzedEvent;
import com.payguard.analytics.event.PaymentCreatedEvent;
import com.payguard.analytics.kafka.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Calls the LLM for a plain-language explanation of a HELD payment. Rules already decided
 * APPROVED/HELD synchronously; this only ever runs after that decision, off the critical path.
 */
@Service
public class FraudAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalysisService.class);

    private final ChatClient chatClient;
    private final EventPublisher eventPublisher;
    private final RateLimitedExecutor rateLimitedExecutor;
    private final AiProperties aiProperties;

    public FraudAnalysisService(ChatClient.Builder chatClientBuilder,
                                 EventPublisher eventPublisher,
                                 RateLimitedExecutor rateLimitedExecutor,
                                 AiProperties aiProperties) {
        this.chatClient = chatClientBuilder.build();
        this.eventPublisher = eventPublisher;
        this.rateLimitedExecutor = rateLimitedExecutor;
        this.aiProperties = aiProperties;
    }

    public void analyzeAsync(PaymentCreatedEvent event, List<String> reasons, Map<String, Object> profileSnapshot) {
        rateLimitedExecutor.submit(() -> analyze(event, reasons, profileSnapshot));
    }

    private void analyze(PaymentCreatedEvent event, List<String> reasons, Map<String, Object> profileSnapshot) {
        PaymentAnalyzedEvent result;
        try {
            FraudAnalysis analysis = callModelWithRetry(event, reasons, profileSnapshot);
            result = new PaymentAnalyzedEvent(UUID.randomUUID(), event.getPaymentId(), Instant.now(), "DONE",
                    analysis.riskLevel().name(), analysis.summary(), analysis.recommendation().name(), analysis.signals());
            log.info("paymentId={} AI analysis done risk={}", event.getPaymentId(), analysis.riskLevel());
        } catch (Exception ex) {
            log.warn("paymentId={} AI analysis failed: {}", event.getPaymentId(), ex.getMessage());
            result = new PaymentAnalyzedEvent(UUID.randomUUID(), event.getPaymentId(), Instant.now(), "FAILED",
                    null, null, null, List.of());
        }
        eventPublisher.publishAnalyzed(event.getPayerVpa(), result);
    }

    private FraudAnalysis callModelWithRetry(PaymentCreatedEvent event, List<String> reasons, Map<String, Object> profileSnapshot) throws Exception {
        int attempts = Math.max(1, aiProperties.getMaxRetries() + 1);
        Exception lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return callModelOnce(event, reasons, profileSnapshot);
            } catch (Exception ex) {
                lastError = ex;
                log.debug("paymentId={} AI attempt {} failed: {}", event.getPaymentId(), attempt, ex.getMessage());
            }
        }
        throw lastError;
    }

    private FraudAnalysis callModelOnce(PaymentCreatedEvent event, List<String> reasons, Map<String, Object> profileSnapshot) throws Exception {
        String prompt = buildPrompt(event, reasons, profileSnapshot);
        CompletableFuture<FraudAnalysis> future = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                .system("You are a fraud-review assistant for a bank. Be concise and concrete.")
                .user(prompt)
                .call()
                .entity(FraudAnalysis.class));
        return future.get(aiProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
    }

    private String buildPrompt(PaymentCreatedEvent event, List<String> reasons, Map<String, Object> profileSnapshot) {
        return """
                A UPI payment was held for review by rule-based fraud checks.

                Payment: ₹%s from %s to %s.
                Rules matched: %s.
                Payer profile: average payment ₹%s, %s known device(s), %s known payee(s), %s payment(s) in the last 10 seconds.

                In 2-3 plain sentences a bank reviewer would understand, explain why this looks risky (or not),
                pick an overall risk level and a recommendation, and list a few short supporting signals.
                """.formatted(
                event.getAmount(), event.getPayerVpa(), event.getPayeeVpa(),
                String.join(", ", reasons),
                profileSnapshot.get("averageAmount"), profileSnapshot.get("knownDeviceCount"),
                profileSnapshot.get("knownPayeeCount"), profileSnapshot.get("recentPaymentCount"));
    }
}
