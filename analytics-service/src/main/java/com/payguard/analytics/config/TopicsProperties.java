package com.payguard.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payguard.topics")
public class TopicsProperties {

    private String paymentCreated = "payment.created";
    private String paymentChecked = "payment.checked";
    private String paymentAnalyzed = "payment.analyzed";
    private String paymentReviewed = "payment.reviewed";

    public String getPaymentCreated() {
        return paymentCreated;
    }

    public void setPaymentCreated(String paymentCreated) {
        this.paymentCreated = paymentCreated;
    }

    public String getPaymentChecked() {
        return paymentChecked;
    }

    public void setPaymentChecked(String paymentChecked) {
        this.paymentChecked = paymentChecked;
    }

    public String getPaymentAnalyzed() {
        return paymentAnalyzed;
    }

    public void setPaymentAnalyzed(String paymentAnalyzed) {
        this.paymentAnalyzed = paymentAnalyzed;
    }

    public String getPaymentReviewed() {
        return paymentReviewed;
    }

    public void setPaymentReviewed(String paymentReviewed) {
        this.paymentReviewed = paymentReviewed;
    }
}
