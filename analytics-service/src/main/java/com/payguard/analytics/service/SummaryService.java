package com.payguard.analytics.service;

import com.payguard.analytics.config.AiProperties;
import com.payguard.analytics.dto.SummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final HeldPaymentsHistory history;
    private final ChatClient chatClient;
    private final AiProperties aiProperties;

    public SummaryService(HeldPaymentsHistory history, ChatClient.Builder chatClientBuilder, AiProperties aiProperties) {
        this.history = history;
        this.chatClient = chatClientBuilder.build();
        this.aiProperties = aiProperties;
    }

    public SummaryResponse summarizeLastHour() {
        List<HeldPaymentsHistory.Entry> entries = history.recentEntries();
        Map<String, Long> countsByRule = new LinkedHashMap<>();
        for (HeldPaymentsHistory.Entry entry : entries) {
            for (String reason : entry.reasons()) {
                countsByRule.merge(reason, 1L, Long::sum);
            }
        }

        if (entries.isEmpty()) {
            return new SummaryResponse("No payments have been held for review in the last hour.", countsByRule, 0);
        }

        String summary;
        try {
            summary = callModel(entries, countsByRule);
        } catch (Exception ex) {
            log.warn("AI summary call failed: {}", ex.getMessage());
            summary = "AI summary is unavailable right now. " + entries.size() + " payment(s) were held in the last hour; see the rule breakdown below.";
        }
        return new SummaryResponse(summary, countsByRule, entries.size());
    }

    private String callModel(List<HeldPaymentsHistory.Entry> entries, Map<String, Long> countsByRule) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Summarise this last hour of UPI payments held for fraud review, in 3-4 plain sentences ")
                .append("a bank reviewer would understand. Mention counts by rule and any notable pattern.\n\n");
        prompt.append("Held payments: ").append(entries.size()).append("\n");
        prompt.append("Counts by rule: ").append(countsByRule).append("\n");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                .system("You are a fraud-review assistant for a bank. Be concise and concrete.")
                .user(prompt.toString())
                .call()
                .content());
        return future.get(aiProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
    }
}
