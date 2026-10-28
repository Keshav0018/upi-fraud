package com.payguard.analytics.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PayerProfileTest {

    @Test
    void usesDefaultAverageUntilThreePaymentsHaveBeenApproved() {
        PayerProfile profile = new PayerProfile();
        assertThat(profile.getEffectiveAverage()).isEqualTo(PayerProfile.DEFAULT_AVERAGE);

        profile.applyApproved(1000, "a@ybl");
        profile.applyApproved(1000, "b@ybl");
        assertThat(profile.getEffectiveAverage()).isEqualTo(PayerProfile.DEFAULT_AVERAGE);

        profile.applyApproved(1000, "c@ybl");
        assertThat(profile.getEffectiveAverage()).isEqualTo(1000.0);
    }

    @Test
    void runningAverageTracksApprovedAmounts() {
        PayerProfile profile = new PayerProfile();
        profile.applyApproved(100, "a@ybl");
        profile.applyApproved(200, "b@ybl");
        profile.applyApproved(300, "c@ybl");
        assertThat(profile.getEffectiveAverage()).isEqualTo(200.0);

        profile.applyApproved(500, "d@ybl");
        assertThat(profile.getEffectiveAverage()).isEqualTo(275.0);
    }

    @Test
    void approvedPaymentRemembersThePayeeOnly() {
        PayerProfile profile = new PayerProfile();
        profile.applyApproved(500, "merchant@ybl");

        assertThat(profile.isKnownPayee("merchant@ybl")).isTrue();
        assertThat(profile.isKnownDevice("device-1")).isFalse();
    }

    @Test
    void deviceIsKnownAfterBeingSeenRegardlessOfOutcome() {
        PayerProfile profile = new PayerProfile();
        assertThat(profile.isKnownDevice("device-1")).isFalse();

        profile.recordDeviceSeen("device-1");

        assertThat(profile.isKnownDevice("device-1")).isTrue();
        assertThat(profile.isKnownPayee("anything@ybl")).isFalse();
    }

    @Test
    void clearedReviewRemembersBothDeviceAndPayee() {
        PayerProfile profile = new PayerProfile();
        profile.applyCleared("device-1", "merchant@ybl");

        assertThat(profile.isKnownDevice("device-1")).isTrue();
        assertThat(profile.isKnownPayee("merchant@ybl")).isTrue();
    }

    @Test
    void recentAttemptsOutsideTheVelocityWindowAreNotCounted() {
        PayerProfile profile = new PayerProfile();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(profile.recordAttemptAndCountRecent(t0)).isEqualTo(1);
        assertThat(profile.recordAttemptAndCountRecent(t0.plusSeconds(2))).isEqualTo(2);
        assertThat(profile.recordAttemptAndCountRecent(t0.plusSeconds(4))).isEqualTo(3);

        // 11 seconds after t0: the first attempt has fallen out of the 10s window
        assertThat(profile.recordAttemptAndCountRecent(t0.plusSeconds(11))).isEqualTo(3);
    }
}
