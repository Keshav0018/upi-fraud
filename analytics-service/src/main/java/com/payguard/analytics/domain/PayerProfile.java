package com.payguard.analytics.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory fraud profile for a single payer VPA. Held entirely in process memory;
 * resets whenever analytics-service restarts (documented limitation, see README).
 */
public class PayerProfile {

    public static final double DEFAULT_AVERAGE = 1500.0;
    public static final int MIN_PAYMENTS_FOR_OWN_AVERAGE = 3;
    public static final Duration VELOCITY_WINDOW = Duration.ofSeconds(10);
    private static final int MAX_TRACKED_ITEMS = 500;

    private final ReentrantLock lock = new ReentrantLock();

    private double averageAmount = 0.0;
    private int approvedCount = 0;
    private final Set<String> knownDeviceIds = new LinkedHashSet<>();
    private final Set<String> knownPayees = new LinkedHashSet<>();
    private final Deque<Instant> recentAttempts = new ArrayDeque<>();

    public double getEffectiveAverage() {
        lock.lock();
        try {
            return approvedCount < MIN_PAYMENTS_FOR_OWN_AVERAGE ? DEFAULT_AVERAGE : averageAmount;
        } finally {
            lock.unlock();
        }
    }

    public boolean isKnownDevice(String deviceId) {
        lock.lock();
        try {
            return knownDeviceIds.contains(deviceId);
        } finally {
            lock.unlock();
        }
    }

    public boolean isKnownPayee(String payeeVpa) {
        lock.lock();
        try {
            return knownPayees.contains(payeeVpa);
        } finally {
            lock.unlock();
        }
    }

    /** Records this payment attempt in the velocity window and returns how many attempts (including this one) fall within it. */
    public int recordAttemptAndCountRecent(Instant timestamp) {
        lock.lock();
        try {
            recentAttempts.addLast(timestamp);
            Instant cutoff = timestamp.minus(VELOCITY_WINDOW);
            while (!recentAttempts.isEmpty() && recentAttempts.peekFirst().isBefore(cutoff)) {
                recentAttempts.pollFirst();
            }
            while (recentAttempts.size() > MAX_TRACKED_ITEMS) {
                recentAttempts.pollFirst();
            }
            return recentAttempts.size();
        } finally {
            lock.unlock();
        }
    }

    /** Applies the running-average and known-payee update that happens for an APPROVED payment. */
    public void applyApproved(double amount, String payeeVpa) {
        lock.lock();
        try {
            averageAmount = (averageAmount * approvedCount + amount) / (approvedCount + 1);
            approvedCount++;
            addPayeeBounded(payeeVpa);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Records that this payer has now used this device at least once, regardless of whether
     * this particular payment is held or approved. Without this, a payer flagged on their very
     * first payment (device AND payee both unavoidably new) could never produce an APPROVED
     * payment afterwards either, since approval itself requires something to already be known —
     * a deadlock. "Known device" means "a device we've seen this payer use before," which is a
     * weaker signal than "known payee" (which stays tied to an actual approved payment, or a
     * reviewer's CLEARED decision, so a fraudulent destination is never blessed just because
     * someone attempted to pay it).
     */
    public void recordDeviceSeen(String deviceId) {
        lock.lock();
        try {
            addDeviceBounded(deviceId);
        } finally {
            lock.unlock();
        }
    }

    /** Applies the trust update that happens when a held payment is reviewed as CLEARED. */
    public void applyCleared(String deviceId, String payeeVpa) {
        lock.lock();
        try {
            addDeviceBounded(deviceId);
            addPayeeBounded(payeeVpa);
        } finally {
            lock.unlock();
        }
    }

    public Snapshot snapshot(int recentPaymentCount) {
        lock.lock();
        try {
            return new Snapshot(getEffectiveAverage(), knownDeviceIds.size(), knownPayees.size(), recentPaymentCount);
        } finally {
            lock.unlock();
        }
    }

    private void addPayeeBounded(String payeeVpa) {
        if (knownPayees.size() >= MAX_TRACKED_ITEMS && !knownPayees.contains(payeeVpa)) {
            knownPayees.remove(knownPayees.iterator().next());
        }
        knownPayees.add(payeeVpa);
    }

    private void addDeviceBounded(String deviceId) {
        if (knownDeviceIds.size() >= MAX_TRACKED_ITEMS && !knownDeviceIds.contains(deviceId)) {
            knownDeviceIds.remove(knownDeviceIds.iterator().next());
        }
        knownDeviceIds.add(deviceId);
    }

    public record Snapshot(double averageAmount, int knownDeviceCount, int knownPayeeCount, int recentPaymentCount) {
    }
}
