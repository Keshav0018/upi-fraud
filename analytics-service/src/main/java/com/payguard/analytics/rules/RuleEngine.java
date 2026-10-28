package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEngine {

    private final List<FraudRule> rules;

    public RuleEngine(List<FraudRule> rules) {
        this.rules = rules;
    }

    public List<String> evaluate(PaymentCreatedEvent event, PayerProfile profile, int recentPaymentCount) {
        return rules.stream()
                .filter(rule -> rule.matches(event, profile, recentPaymentCount))
                .map(FraudRule::code)
                .toList();
    }
}
