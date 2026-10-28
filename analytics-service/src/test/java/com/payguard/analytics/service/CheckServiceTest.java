package com.payguard.analytics.service;

import com.payguard.analytics.ai.FraudAnalysisService;
import com.payguard.analytics.event.PaymentCheckedEvent;
import com.payguard.analytics.event.PaymentCreatedEvent;
import com.payguard.analytics.kafka.EventPublisher;
import com.payguard.analytics.rules.LargeAmountRule;
import com.payguard.analytics.rules.NewDeviceNewPayeeRule;
import com.payguard.analytics.rules.RuleEngine;
import com.payguard.analytics.rules.VelocityRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CheckServiceTest {

    private ProfileService profileService;
    private EventPublisher eventPublisher;
    private HeldPaymentsHistory heldPaymentsHistory;
    private FraudAnalysisService fraudAnalysisService;
    private CheckService checkService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService();
        RuleEngine ruleEngine = new RuleEngine(List.of(new LargeAmountRule(), new VelocityRule(), new NewDeviceNewPayeeRule()));
        eventPublisher = mock(EventPublisher.class);
        heldPaymentsHistory = mock(HeldPaymentsHistory.class);
        fraudAnalysisService = mock(FraudAnalysisService.class);
        checkService = new CheckService(profileService, ruleEngine, eventPublisher, heldPaymentsHistory, fraudAnalysisService);
    }

    private PaymentCreatedEvent event(String payerVpa, String payeeVpa, String amount, String deviceId) {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(UUID.randomUUID());
        event.setTimestamp(Instant.now());
        event.setPayerVpa(payerVpa);
        event.setPayeeVpa(payeeVpa);
        event.setAmount(new BigDecimal(amount));
        event.setDeviceId(deviceId);
        return event;
    }

    @Test
    void approvesAnOrdinaryPaymentAndUpdatesTheProfile() {
        // seed the profile as if this device/payee pair were already trusted, so this
        // payment is only exercising the "ordinary, nothing flagged" path
        profileService.getOrCreate("payer@ybl").applyCleared("device-1", "merchant@ybl");

        PaymentCreatedEvent event = event("payer@ybl", "merchant@ybl", "500", "device-1");

        checkService.handle(event);

        ArgumentCaptor<PaymentCheckedEvent> captor = ArgumentCaptor.forClass(PaymentCheckedEvent.class);
        verify(eventPublisher).publishChecked(eq("payer@ybl"), captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getReasons()).isEmpty();

        verify(fraudAnalysisService, never()).analyzeAsync(any(), any(), any());
        verify(heldPaymentsHistory, never()).record(any(), any());

        assertThat(profileService.getOrCreate("payer@ybl").isKnownPayee("merchant@ybl")).isTrue();
    }

    @Test
    void holdsALargePaymentAndTriggersAiAnalysis() {
        PaymentCreatedEvent event = event("payer@ybl", "merchant@ybl", "20000", "device-1");

        checkService.handle(event);

        ArgumentCaptor<PaymentCheckedEvent> captor = ArgumentCaptor.forClass(PaymentCheckedEvent.class);
        verify(eventPublisher).publishChecked(eq("payer@ybl"), captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("HELD");
        assertThat(captor.getValue().getReasons()).contains("LARGE_AMOUNT");

        verify(fraudAnalysisService).analyzeAsync(eq(event), eq(captor.getValue().getReasons()), any(Map.class));
        verify(heldPaymentsHistory).record(eq(event), eq(captor.getValue().getReasons()));

        // a held payment must not be treated as a trustworthy signal for the profile
        assertThat(profileService.getOrCreate("payer@ybl").isKnownPayee("merchant@ybl")).isFalse();
    }

    @Test
    void aColdStartPayerCanStillReachApprovedOnTheirSecondPayment() {
        // first ever payment: device and payee are both unavoidably new -> held
        PaymentCreatedEvent first = event("new.payer@ybl", "payee.one@ybl", "500", "device-1");
        checkService.handle(first);

        // second payment, same device, a *different* new payee: device is now known from the
        // first attempt, so this must NOT be stuck held forever just because the payee is new
        PaymentCreatedEvent second = event("new.payer@ybl", "payee.two@ybl", "500", "device-1");
        checkService.handle(second);

        ArgumentCaptor<PaymentCheckedEvent> captor = ArgumentCaptor.forClass(PaymentCheckedEvent.class);
        verify(eventPublisher, times(2)).publishChecked(eq("new.payer@ybl"), captor.capture());
        List<PaymentCheckedEvent> checked = captor.getAllValues();

        assertThat(checked.get(0).getResult()).isEqualTo("HELD");
        assertThat(checked.get(1).getResult()).isEqualTo("APPROVED");
    }
}
