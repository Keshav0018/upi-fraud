package com.payguard.payment.repository;

import com.payguard.payment.domain.AlertStatus;
import com.payguard.payment.domain.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    List<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    Optional<FraudAlert> findByPaymentId(UUID paymentId);

    long countByStatus(AlertStatus status);
}
