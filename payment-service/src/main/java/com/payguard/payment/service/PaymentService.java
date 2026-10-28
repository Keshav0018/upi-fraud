package com.payguard.payment.service;

import com.payguard.payment.domain.FraudAlert;
import com.payguard.payment.domain.Payment;
import com.payguard.payment.domain.PaymentStatus;
import com.payguard.payment.domain.ProcessedEvent;
import com.payguard.payment.dto.PaymentRequest;
import com.payguard.payment.dto.PaymentResponse;
import com.payguard.payment.event.PaymentAnalyzedEvent;
import com.payguard.payment.event.PaymentCheckedEvent;
import com.payguard.payment.event.PaymentCreatedEvent;
import com.payguard.payment.exception.InvalidStateException;
import com.payguard.payment.exception.NotFoundException;
import com.payguard.payment.repository.FraudAlertRepository;
import com.payguard.payment.repository.PaymentRepository;
import com.payguard.payment.repository.ProcessedEventRepository;
import com.payguard.payment.sse.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SsePublisher ssePublisher;

    public PaymentService(PaymentRepository paymentRepository,
                           FraudAlertRepository fraudAlertRepository,
                           ProcessedEventRepository processedEventRepository,
                           ApplicationEventPublisher applicationEventPublisher,
                           SsePublisher ssePublisher) {
        this.paymentRepository = paymentRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.processedEventRepository = processedEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.ssePublisher = ssePublisher;
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        Payment payment = Payment.newPending(request.getPayerVpa(), request.getPayeeVpa(),
                request.getAmount(), request.getNote(), request.getDeviceId());
        paymentRepository.save(payment);
        applicationEventPublisher.publishEvent(PaymentCreatedEvent.of(
                payment.getId(), payment.getPayerVpa(), payment.getPayeeVpa(),
                payment.getAmount(), payment.getDeviceId()));
        log.info("paymentId={} created PENDING_CHECK payerVpa={}", payment.getId(), payment.getPayerVpa());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return PaymentResponse.from(findPaymentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(String payerVpa) {
        List<Payment> payments = payerVpa != null
                ? paymentRepository.findByPayerVpaOrderByCreatedAtDesc(payerVpa)
                : paymentRepository.findTop50ByOrderByCreatedAtDesc();
        return payments.stream().map(PaymentResponse::from).toList();
    }

    @Transactional
    public PaymentResponse updateNote(UUID id, String note) {
        Payment payment = findPaymentOrThrow(id);
        payment.setNote(note);
        payment.touch();
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void deletePayment(UUID id) {
        Payment payment = findPaymentOrThrow(id);
        if (payment.getStatus() != PaymentStatus.PENDING_CHECK) {
            throw new InvalidStateException("Only a payment still PENDING_CHECK can be deleted");
        }
        paymentRepository.delete(payment);
    }

    @Transactional
    public void applyCheckedResult(PaymentCheckedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("paymentId={} eventId={} already processed, skipping", event.getPaymentId(), event.getEventId());
            return;
        }

        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new NotFoundException("Payment not found: " + event.getPaymentId()));

        boolean held = "HELD".equals(event.getResult());
        payment.setStatus(held ? PaymentStatus.HELD : PaymentStatus.COMPLETED);
        payment.setCheckLatencyMs(event.getCheckLatencyMs());
        payment.touch();

        FraudAlert alert = null;
        if (held) {
            alert = FraudAlert.open(payment.getId(), payment.getPayerVpa(), payment.getPayeeVpa(),
                    payment.getAmount(), event.getReasons(), event.getProfileSnapshot());
            fraudAlertRepository.save(alert);
        }

        processedEventRepository.save(ProcessedEvent.of(event.getEventId(), "payment.checked"));

        log.info("paymentId={} checked result={} latencyMs={}", payment.getId(), event.getResult(), event.getCheckLatencyMs());

        ssePublisher.broadcast("payment.checked", PaymentResponse.from(payment));
        if (alert != null) {
            ssePublisher.broadcast("alert.created", com.payguard.payment.dto.AlertResponse.from(alert));
        }
    }

    @Transactional
    public void applyAnalyzedResult(PaymentAnalyzedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("paymentId={} eventId={} already processed, skipping", event.getPaymentId(), event.getEventId());
            return;
        }

        FraudAlert alert = fraudAlertRepository.findByPaymentId(event.getPaymentId()).orElse(null);
        if (alert == null) {
            log.warn("paymentId={} received PaymentAnalyzedEvent but no fraud alert exists", event.getPaymentId());
            processedEventRepository.save(ProcessedEvent.of(event.getEventId(), "payment.analyzed"));
            return;
        }

        alert.setAiStatus(com.payguard.payment.domain.AiStatus.valueOf(event.getAiStatus()));
        alert.setAiRiskLevel(event.getRiskLevel());
        alert.setAiSummary(event.getSummary());
        alert.setAiRecommendation(event.getRecommendation());
        alert.setAiSignals(event.getSignals());

        processedEventRepository.save(ProcessedEvent.of(event.getEventId(), "payment.analyzed"));

        log.info("paymentId={} AI analysis attached status={} risk={}", event.getPaymentId(), event.getAiStatus(), event.getRiskLevel());

        ssePublisher.broadcast("alert.analyzed", com.payguard.payment.dto.AlertResponse.from(alert));
    }

    Payment findPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + id));
    }
}
