package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LargeAmountRuleTest {

    private final LargeAmountRule rule = new LargeAmountRule();

    private PaymentCreatedEvent eventWithAmount(String amount) {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setAmount(new BigDecimal(amount));
        return event;
    }

    @Test
    void matchesWhenAboveFiveTimesDefaultAverageAndAboveTenThousand() {
        // default average is 1500, so 5x = 7500; amount must also exceed 10000
        PayerProfile profile = new PayerProfile();
        assertThat(rule.matches(eventWithAmount("15000"), profile, 1)).isTrue();
    }

    @Test
    void doesNotMatchWhenBelowTenThousandEvenIfAboveFiveTimesAverage() {
        PayerProfile profile = new PayerProfile();
        profile.applyApproved(100, "a@ybl");
        profile.applyApproved(100, "b@ybl");
        profile.applyApproved(100, "c@ybl"); // average now 100, 5x = 500
        assertThat(rule.matches(eventWithAmount("9000"), profile, 1)).isFalse();
    }

    @Test
    void matchesWhenAboveFiveTimesALowLearnedAverage() {
        PayerProfile profile = new PayerProfile();
        profile.applyApproved(100, "a@ybl");
        profile.applyApproved(100, "b@ybl");
        profile.applyApproved(100, "c@ybl"); // average now 100, 5x = 500, floor is 10000
        assertThat(rule.matches(eventWithAmount("10001"), profile, 1)).isTrue();
    }

    @Test
    void doesNotMatchAnOrdinaryPayment() {
        PayerProfile profile = new PayerProfile();
        assertThat(rule.matches(eventWithAmount("500"), profile, 1)).isFalse();
    }
}
