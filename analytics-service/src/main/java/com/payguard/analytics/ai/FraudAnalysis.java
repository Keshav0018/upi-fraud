package com.payguard.analytics.ai;

import java.util.List;

/** Structured output shape asked of the LLM for a single HELD payment. */
public record FraudAnalysis(RiskLevel riskLevel, String summary, Recommendation recommendation, List<String> signals) {

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public enum Recommendation { BLOCK, REVIEW, ALLOW }
}
