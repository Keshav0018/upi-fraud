package com.payguard.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payguard.simulator")
public class SimulatorProperties {

    private boolean enabled = false;
    private double ratePerSec = 1.0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getRatePerSec() {
        return ratePerSec;
    }

    public void setRatePerSec(double ratePerSec) {
        this.ratePerSec = ratePerSec;
    }
}
