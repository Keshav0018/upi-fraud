package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

/** Flags a payment from a device the payer has never used, to a payee the payer has never paid. */
@Component
public class NewDeviceNewPayeeRule implements FraudRule {

    @Override
    public String code() {
        return "NEW_DEVICE_NEW_PAYEE";
    }

    @Override
    public boolean matches(PaymentCreatedEvent event, PayerProfile profile, int recentPaymentCount) {
        return !profile.isKnownDevice(event.getDeviceId()) && !profile.isKnownPayee(event.getPayeeVpa());
    }
}
