package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewDeviceNewPayeeRuleTest {

    private final NewDeviceNewPayeeRule rule = new NewDeviceNewPayeeRule();

    private PaymentCreatedEvent event(String deviceId, String payeeVpa) {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setDeviceId(deviceId);
        event.setPayeeVpa(payeeVpa);
        return event;
    }

    @Test
    void matchesWhenBothDeviceAndPayeeAreNew() {
        PayerProfile profile = new PayerProfile();
        assertThat(rule.matches(event("device-1", "merchant@ybl"), profile, 1)).isTrue();
    }

    @Test
    void doesNotMatchWhenDeviceIsKnown() {
        PayerProfile profile = new PayerProfile();
        profile.applyCleared("device-1", "someone-else@ybl");
        assertThat(rule.matches(event("device-1", "merchant@ybl"), profile, 1)).isFalse();
    }

    @Test
    void doesNotMatchWhenPayeeIsKnown() {
        PayerProfile profile = new PayerProfile();
        profile.applyApproved(500, "merchant@ybl");
        assertThat(rule.matches(event("device-1", "merchant@ybl"), profile, 1)).isFalse();
    }
}
