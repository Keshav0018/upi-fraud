package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityRuleTest {

    private final VelocityRule rule = new VelocityRule();
    private final PaymentCreatedEvent event = new PaymentCreatedEvent();
    private final PayerProfile profile = new PayerProfile();

    @Test
    void doesNotMatchBelowThreeRecentPayments() {
        assertThat(rule.matches(event, profile, 1)).isFalse();
        assertThat(rule.matches(event, profile, 2)).isFalse();
    }

    @Test
    void matchesAtThreeOrMoreRecentPayments() {
        assertThat(rule.matches(event, profile, 3)).isTrue();
        assertThat(rule.matches(event, profile, 4)).isTrue();
    }
}
