package com.payguard.analytics.dto;

import java.util.Map;

public class SummaryResponse {
    private final String summary;
    private final Map<String, Long> countsByRule;
    private final int heldCount;

    public SummaryResponse(String summary, Map<String, Long> countsByRule, int heldCount) {
        this.summary = summary;
        this.countsByRule = countsByRule;
        this.heldCount = heldCount;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Long> getCountsByRule() {
        return countsByRule;
    }

    public int getHeldCount() {
        return heldCount;
    }
}
