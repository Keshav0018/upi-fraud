package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

/** Flags a payer sending 3 or more payments within a 10-second window. */
@Component
public class VelocityRule implements FraudRule {

    private static final int THRESHOLD = 3;

    @Override
    public String code() {
        return "VELOCITY";
    }

    @Override
    public boolean matches(PaymentCreatedEvent event, PayerProfile profile, int recentPaymentCount) {
        return recentPaymentCount >= THRESHOLD;
    }
}
