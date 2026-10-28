package com.payguard.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.analytics.event.PaymentCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Kafka round trip for a HELD payment. No LLM credentials are configured here,
 * so the AI call fails fast (401 from the real Groq endpoint) and this also proves the
 * "publish aiStatus=FAILED on failure" path actually works end to end.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AnalyticsFlowIntegrationTest {

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka = new org.testcontainers.kafka.KafkaContainer("apache/kafka:4.0.0");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // Deliberately not a real key: this proves the aiStatus=FAILED fallback path works
        // without needing real LLM credentials in CI. Spring AI validates non-blank eagerly at startup.
        registry.add("spring.ai.openai.api-key", () -> "test-key-not-real");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaConsumer<String, String> rawConsumer;

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        rawConsumer = new KafkaConsumer<>(props);
        rawConsumer.subscribe(List.of("payment.checked", "payment.analyzed"));
    }

    @AfterEach
    void tearDownConsumer() {
        rawConsumer.close();
    }

    @Test
    void heldPaymentIsCheckedAndAnalysisFailsWithoutLlmCredentials() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(UUID.randomUUID());
        event.setTimestamp(Instant.now());
        event.setPayerVpa("payer@ybl");
        event.setPayeeVpa("merchant@ybl");
        event.setAmount(new BigDecimal("50000"));
        event.setDeviceId("device-1");

        kafkaTemplate.send("payment.created", event.getPayerVpa(), event).get(10, java.util.concurrent.TimeUnit.SECONDS);

        JsonNode checked = null;
        JsonNode analyzed = null;
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline) && (checked == null || analyzed == null)) {
            ConsumerRecords<String, String> records = rawConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode node = objectMapper.readTree(record.value());
                if (record.topic().equals("payment.checked") && node.has("paymentId")
                        && node.get("paymentId").asText().equals(event.getPaymentId().toString())) {
                    checked = node;
                } else if (record.topic().equals("payment.analyzed") && node.has("paymentId")
                        && node.get("paymentId").asText().equals(event.getPaymentId().toString())) {
                    analyzed = node;
                }
            }
        }

        assertThat(checked).as("payment.checked message").isNotNull();
        assertThat(checked.get("result").asText()).isEqualTo("HELD");
        assertThat(checked.get("reasons").toString()).contains("LARGE_AMOUNT");

        assertThat(analyzed).as("payment.analyzed message").isNotNull();
        assertThat(analyzed.get("aiStatus").asText()).isEqualTo("FAILED");
    }
}
