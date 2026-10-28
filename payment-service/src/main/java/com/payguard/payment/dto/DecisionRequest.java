package com.payguard.payment.dto;

import jakarta.validation.constraints.NotNull;

public class DecisionRequest {

    public enum Decision { FRAUD, CLEARED }

    @NotNull(message = "decision is required (FRAUD or CLEARED)")
    private Decision decision;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
