package com.payguard.payment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentRequest {

    public static final String VPA_REGEX = "^[a-z0-9._-]{2,}@[a-z]{2,}$";

    @NotBlank(message = "payerVpa is required")
    @Pattern(regexp = VPA_REGEX, message = "payerVpa must look like a UPI VPA, e.g. keshav.chandak@oksbi")
    private String payerVpa;

    @NotBlank(message = "payeeVpa is required")
    @Pattern(regexp = VPA_REGEX, message = "payeeVpa must look like a UPI VPA, e.g. keshav.chandak@oksbi")
    private String payeeVpa;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "1", message = "amount must be at least ₹1")
    @DecimalMax(value = "100000", message = "amount must not exceed ₹1,00,000")
    private BigDecimal amount;

    @Size(max = 280, message = "note must be at most 280 characters")
    private String note;

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    public String getPayerVpa() {
        return payerVpa;
    }

    public void setPayerVpa(String payerVpa) {
        this.payerVpa = payerVpa;
    }

    public String getPayeeVpa() {
        return payeeVpa;
    }

    public void setPayeeVpa(String payeeVpa) {
        this.payeeVpa = payeeVpa;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
