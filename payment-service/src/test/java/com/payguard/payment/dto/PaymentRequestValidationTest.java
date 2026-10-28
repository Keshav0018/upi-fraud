package com.payguard.payment.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private PaymentRequest validRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setPayerVpa("keshav.chandak@oksbi");
        request.setPayeeVpa("shop.owner@ybl");
        request.setAmount(new BigDecimal("500"));
        request.setDeviceId("device-1");
        return request;
    }

    @Test
    void acceptsAWellFormedRequest() {
        assertThat(VALIDATOR.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsUppercaseVpa() {
        PaymentRequest request = validRequest();
        request.setPayerVpa("Keshav.Chandak@OKSBI");
        Set<ConstraintViolation<PaymentRequest>> violations = VALIDATOR.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsVpaWithoutAtSign() {
        PaymentRequest request = validRequest();
        request.setPayeeVpa("notavpa");
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsAmountBelowOneRupee() {
        PaymentRequest request = validRequest();
        request.setAmount(new BigDecimal("0.50"));
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsAmountAboveOneLakh() {
        PaymentRequest request = validRequest();
        request.setAmount(new BigDecimal("100001"));
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsBlankDeviceId() {
        PaymentRequest request = validRequest();
        request.setDeviceId(" ");
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }
}
