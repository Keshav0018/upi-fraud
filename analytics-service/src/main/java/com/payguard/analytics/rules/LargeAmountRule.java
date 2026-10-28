package com.payguard.analytics.rules;

import com.payguard.analytics.domain.PayerProfile;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Flags a payment far above the payer's usual spend: more than 5x their average AND over ₹10,000. */
@Component
public class LargeAmountRule implements FraudRule {

    private static final BigDecimal MULTIPLIER = BigDecimal.valueOf(5);
    private static final BigDecimal ABSOLUTE_FLOOR = BigDecimal.valueOf(10000);

    @Override
    public String code() {
        return "LARGE_AMOUNT";
    }

    @Override
    public boolean matches(PaymentCreatedEvent event, PayerProfile profile, int recentPaymentCount) {
        BigDecimal amount = event.getAmount();
        BigDecimal average = BigDecimal.valueOf(profile.getEffectiveAverage());
        BigDecimal fiveTimesAverage = average.multiply(MULTIPLIER);
        return amount.compareTo(fiveTimesAverage) > 0 && amount.compareTo(ABSOLUTE_FLOOR) > 0;
    }
}
