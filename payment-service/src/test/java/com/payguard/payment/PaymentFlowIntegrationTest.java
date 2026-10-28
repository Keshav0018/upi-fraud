package com.payguard.payment;

import com.payguard.payment.domain.AiStatus;
import com.payguard.payment.domain.AlertStatus;
import com.payguard.payment.domain.FraudAlert;
import com.payguard.payment.domain.PaymentStatus;
import com.payguard.payment.dto.AlertResponse;
import com.payguard.payment.dto.DecisionRequest;
import com.payguard.payment.dto.PaymentRequest;
import com.payguard.payment.dto.PaymentResponse;
import com.payguard.payment.event.PaymentAnalyzedEvent;
import com.payguard.payment.event.PaymentCheckedEvent;
import com.payguard.payment.repository.FraudAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("payments_db");

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer("apache/kafka:4.0.0");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payguard.simulator.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Test
    void paymentIsCreatedHeldAnalyzedAndReviewed() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPayerVpa("keshav.chandak@oksbi");
        request.setPayeeVpa("newmerchant@okaxis");
        request.setAmount(new BigDecimal("35000"));
        request.setNote("large payment demo");
        request.setDeviceId("device-123");

        ResponseEntity<PaymentResponse> created = restTemplate.postForEntity("/api/payments", request, PaymentResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PaymentResponse body = created.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(PaymentStatus.PENDING_CHECK);
        UUID paymentId = body.getId();

        PaymentCheckedEvent checkedEvent = new PaymentCheckedEvent();
        checkedEvent.setEventId(UUID.randomUUID());
        checkedEvent.setPaymentId(paymentId);
        checkedEvent.setTimestamp(Instant.now());
        checkedEvent.setResult("HELD");
        checkedEvent.setReasons(List.of("LARGE_AMOUNT"));
        checkedEvent.setCheckLatencyMs(42L);
        checkedEvent.setProfileSnapshot(Map.of("averageAmount", 1500, "knownDeviceCount", 1));
        kafkaTemplate.send("payment.checked", request.getPayerVpa(), checkedEvent).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ResponseEntity<PaymentResponse> fetched = restTemplate.getForEntity("/api/payments/{id}", PaymentResponse.class, paymentId);
            assertThat(fetched.getBody().getStatus()).isEqualTo(PaymentStatus.HELD);
        });

        FraudAlert alert = await().atMost(Duration.ofSeconds(10))
                .until(() -> fraudAlertRepository.findByPaymentId(paymentId).orElse(null), a -> a != null);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.PENDING);
        assertThat(alert.getReasons()).contains("LARGE_AMOUNT");

        PaymentAnalyzedEvent analyzedEvent = new PaymentAnalyzedEvent();
        analyzedEvent.setEventId(UUID.randomUUID());
        analyzedEvent.setPaymentId(paymentId);
        analyzedEvent.setTimestamp(Instant.now());
        analyzedEvent.setAiStatus("DONE");
        analyzedEvent.setRiskLevel("HIGH");
        analyzedEvent.setSummary("Unusually large payment to a new payee.");
        analyzedEvent.setRecommendation("BLOCK");
        analyzedEvent.setSignals(List.of("amount far above average"));
        kafkaTemplate.send("payment.analyzed", request.getPayerVpa(), analyzedEvent).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            FraudAlert updated = fraudAlertRepository.findByPaymentId(paymentId).orElseThrow();
            assertThat(updated.getAiStatus()).isEqualTo(AiStatus.DONE);
            assertThat(updated.getAiRiskLevel()).isEqualTo("HIGH");
        });

        DecisionRequest decision = new DecisionRequest();
        decision.setDecision(DecisionRequest.Decision.CLEARED);
        ResponseEntity<AlertResponse> decided = restTemplate.postForEntity(
                "/api/alerts/{id}/decision", decision, AlertResponse.class, alert.getId());
        assertThat(decided.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(decided.getBody().getStatus()).isEqualTo(AlertStatus.CLEARED);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<PaymentResponse> fetched = restTemplate.getForEntity("/api/payments/{id}", PaymentResponse.class, paymentId);
            assertThat(fetched.getBody().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });
    }
}
