package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;

public interface FraudRule {

    String code();

    boolean matches(PaymentCreatedEvent event, PayerProfile profile, int recentPaymentCount);
}
