package com.payguard.payment.service;

import com.payguard.payment.config.SimulatorProperties;
import com.payguard.payment.dto.PaymentRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Generates background UPI traffic so the dashboard has something to show without a human
 * driving the upi-app. Roughly 7% of generated payments are shaped to trip one of the
 * fraud rules in analytics-service.
 */
@Component
@ConditionalOnProperty(prefix = "payguard.simulator", name = "enabled", havingValue = "true")
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);
    private static final double FRAUD_RATE = 0.07;

    private record FakeUser(String name, String vpa, String deviceId) {
    }

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Krishna", "Ishaan", "Shaurya",
            "Ananya", "Diya", "Saanvi", "Aadhya", "Kavya", "Myra", "Anika", "Riya", "Siya", "Pari",
            "Rohan", "Karan", "Nikhil", "Rahul", "Amit", "Vikram", "Suresh", "Rajesh", "Manoj", "Deepak",
            "Priya", "Neha", "Pooja", "Sneha", "Shreya", "Kritika", "Isha", "Tanvi", "Nisha", "Meera"
    };
    private static final String[] BANK_SUFFIXES = {"@okhdfcbank", "@ybl", "@paytm", "@oksbi", "@okaxis"};
    private static final String[] MERCHANT_PAYEES = {
            "swiggy.pay@ybl", "zomato.pay@paytm", "amazon.pay@apl", "bigbasket@okaxis",
            "irctc.pay@oksbi", "electricitybill@okhdfcbank", "flipkart.pay@ybl", "dmart@paytm"
    };

    private final SimulatorProperties properties;
    private final PaymentService paymentService;
    private final Random random = new Random();
    private final List<FakeUser> users;

    private ScheduledExecutorService executor;

    public SimulatorService(SimulatorProperties properties, PaymentService paymentService) {
        this.properties = properties;
        this.paymentService = paymentService;
        this.users = buildFakeUsers();
    }

    private List<FakeUser> buildFakeUsers() {
        return java.util.stream.IntStream.range(0, FIRST_NAMES.length)
                .mapToObj(i -> {
                    String handle = FIRST_NAMES[i].toLowerCase() + (100 + i);
                    String vpa = handle + BANK_SUFFIXES[i % BANK_SUFFIXES.length];
                    return new FakeUser(FIRST_NAMES[i], vpa, "device-" + UUID.randomUUID());
                })
                .toList();
    }

    @PostConstruct
    void start() {
        long periodMillis = Math.max(20L, Math.round(1000.0 / Math.max(0.01, properties.getRatePerSec())));
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "payment-simulator");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::tick, 2000, periodMillis, TimeUnit.MILLISECONDS);
        log.info("Simulator started: ~{} payments/sec", properties.getRatePerSec());
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void tick() {
        try {
            if (random.nextDouble() < FRAUD_RATE) {
                generateFraudShapedPayment();
            } else {
                generateNormalPayment();
            }
        } catch (Exception ex) {
            log.warn("Simulator tick failed: {}", ex.getMessage());
        }
    }

    private void generateNormalPayment() {
        FakeUser payer = randomUser();
        String payee = random.nextBoolean() ? randomUser().vpa() : randomMerchant();
        BigDecimal amount = BigDecimal.valueOf(50 + random.nextInt(3000));
        submit(payer.vpa(), payee, amount, payer.deviceId(), "simulated payment");
    }

    private void generateFraudShapedPayment() {
        FakeUser payer = randomUser();
        int pattern = random.nextInt(3);
        switch (pattern) {
            case 0 -> {
                // LARGE_AMOUNT
                BigDecimal amount = BigDecimal.valueOf(15000 + random.nextInt(45000));
                submit(payer.vpa(), randomMerchant(), amount, payer.deviceId(), "large payment");
            }
            case 1 -> {
                // VELOCITY: three rapid payments from the same payer
                for (int i = 0; i < 3; i++) {
                    BigDecimal amount = BigDecimal.valueOf(200 + random.nextInt(800));
                    submit(payer.vpa(), randomMerchant(), amount, payer.deviceId(), "rapid payment " + (i + 1));
                    sleepQuietly(300);
                }
            }
            default -> {
                // NEW_DEVICE_NEW_PAYEE
                String newDevice = "device-" + UUID.randomUUID();
                String newPayee = "newpayee" + random.nextInt(100000) + "@okaxis";
                BigDecimal amount = BigDecimal.valueOf(500 + random.nextInt(4000));
                submit(payer.vpa(), newPayee, amount, newDevice, "new device payment");
            }
        }
    }

    private void submit(String payerVpa, String payeeVpa, BigDecimal amount, String deviceId, String note) {
        PaymentRequest request = new PaymentRequest();
        request.setPayerVpa(payerVpa);
        request.setPayeeVpa(payeeVpa);
        request.setAmount(amount);
        request.setDeviceId(deviceId);
        request.setNote(note);
        paymentService.createPayment(request);
    }

    private FakeUser randomUser() {
        return users.get(random.nextInt(users.size()));
    }

    private String randomMerchant() {
        return MERCHANT_PAYEES[random.nextInt(MERCHANT_PAYEES.length)];
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
