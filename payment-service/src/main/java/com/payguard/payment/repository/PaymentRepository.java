package com.payguard.payment.repository;

import com.payguard.payment.domain.Payment;
import com.payguard.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByPayerVpaOrderByCreatedAtDesc(String payerVpa);

    List<Payment> findTop50ByOrderByCreatedAtDesc();

    long countByStatusNot(PaymentStatus status);

    List<Payment> findByCheckLatencyMsIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    @Query("select avg(p.checkLatencyMs) from Payment p where p.checkLatencyMs is not null")
    Double averageCheckLatencyMs();
}
