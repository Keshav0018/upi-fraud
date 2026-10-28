package com.payguard.payment.service;

import com.payguard.payment.domain.Payment;
import com.payguard.payment.domain.PaymentStatus;
import com.payguard.payment.dto.StatsResponse;
import com.payguard.payment.repository.FraudAlertRepository;
import com.payguard.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private static final int INTERVAL_SECONDS = 5;
    private static final int INTERVAL_COUNT = 30;

    private final PaymentRepository paymentRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public StatsService(PaymentRepository paymentRepository, FraudAlertRepository fraudAlertRepository) {
        this.paymentRepository = paymentRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse computeStats() {
        long checkedCount = paymentRepository.countByStatusNot(PaymentStatus.PENDING_CHECK);
        long flaggedCount = fraudAlertRepository.count();
        double flagRate = checkedCount == 0 ? 0.0 : (double) flaggedCount / checkedCount;
        Double avgLatency = paymentRepository.averageCheckLatencyMs();

        Map<String, Long> countsByRule = new LinkedHashMap<>();
        for (var alert : fraudAlertRepository.findAll()) {
            for (String reason : alert.getReasons()) {
                countsByRule.merge(reason, 1L, Long::sum);
            }
        }

        List<StatsResponse.ThroughputPoint> throughput = computeThroughput();

        return new StatsResponse(checkedCount, flaggedCount, flagRate,
                avgLatency == null ? 0.0 : avgLatency, countsByRule, throughput);
    }

    private List<StatsResponse.ThroughputPoint> computeThroughput() {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds((long) INTERVAL_SECONDS * INTERVAL_COUNT + 10);

        List<Payment> recent = paymentRepository
                .findByCheckLatencyMsIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(windowStart);

        long[] buckets = new long[INTERVAL_COUNT];
        long nowEpoch = now.getEpochSecond();

        for (Payment payment : recent) {
            Instant checkedAt = payment.getCreatedAt().plusMillis(payment.getCheckLatencyMs());
            long ageSeconds = nowEpoch - checkedAt.getEpochSecond();
            int bucketIndex = (int) (ageSeconds / INTERVAL_SECONDS);
            if (bucketIndex >= 0 && bucketIndex < INTERVAL_COUNT) {
                buckets[INTERVAL_COUNT - 1 - bucketIndex]++;
            }
        }

        List<StatsResponse.ThroughputPoint> points = new ArrayList<>(INTERVAL_COUNT);
        Instant firstIntervalStart = now.minusSeconds((long) INTERVAL_SECONDS * (INTERVAL_COUNT - 1))
                .truncatedTo(ChronoUnit.SECONDS);
        for (int i = 0; i < INTERVAL_COUNT; i++) {
            Instant intervalStart = firstIntervalStart.plusSeconds((long) i * INTERVAL_SECONDS);
            points.add(new StatsResponse.ThroughputPoint(intervalStart, buckets[i]));
        }
        return points;
    }
}
