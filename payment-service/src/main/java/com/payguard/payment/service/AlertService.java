package com.payguard.payment.service;

import com.payguard.payment.domain.AlertStatus;
import com.payguard.payment.domain.FraudAlert;
import com.payguard.payment.domain.Payment;
import com.payguard.payment.domain.PaymentStatus;
import com.payguard.payment.dto.AlertResponse;
import com.payguard.payment.dto.DecisionRequest;
import com.payguard.payment.event.PaymentReviewedEvent;
import com.payguard.payment.exception.InvalidStateException;
import com.payguard.payment.exception.NotFoundException;
import com.payguard.payment.repository.FraudAlertRepository;
import com.payguard.payment.repository.PaymentRepository;
import com.payguard.payment.sse.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SsePublisher ssePublisher;

    public AlertService(FraudAlertRepository fraudAlertRepository,
                         PaymentRepository paymentRepository,
                         ApplicationEventPublisher applicationEventPublisher,
                         SsePublisher ssePublisher) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.paymentRepository = paymentRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.ssePublisher = ssePublisher;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> listAlerts(AlertStatus status) {
        List<FraudAlert> alerts = status != null
                ? fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status)
                : fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        return alerts.stream().map(AlertResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlert(UUID id) {
        return AlertResponse.from(findAlertOrThrow(id));
    }

    @Transactional
    public AlertResponse decide(UUID id, DecisionRequest request) {
        FraudAlert alert = findAlertOrThrow(id);
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new InvalidStateException("Alert " + id + " has already been reviewed");
        }

        Payment payment = paymentRepository.findById(alert.getPaymentId())
                .orElseThrow(() -> new NotFoundException("Payment not found: " + alert.getPaymentId()));
        boolean fraud = request.getDecision() == DecisionRequest.Decision.FRAUD;

        alert.setStatus(fraud ? AlertStatus.FRAUD : AlertStatus.CLEARED);
        alert.setReviewedAt(Instant.now());

        payment.setStatus(fraud ? PaymentStatus.BLOCKED : PaymentStatus.COMPLETED);
        payment.touch();

        applicationEventPublisher.publishEvent(PaymentReviewedEvent.of(
                payment.getId(), alert.getStatus().name(), payment.getPayerVpa(), payment.getPayeeVpa(), payment.getDeviceId()));

        log.info("paymentId={} alertId={} reviewed decision={}", payment.getId(), alert.getId(), alert.getStatus());

        AlertResponse response = AlertResponse.from(alert);
        ssePublisher.broadcast("alert.decided", response);
        ssePublisher.broadcast("payment.checked", com.payguard.payment.dto.PaymentResponse.from(payment));
        return response;
    }

    private FraudAlert findAlertOrThrow(UUID id) {
        return fraudAlertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fraud alert not found: " + id));
    }
}
