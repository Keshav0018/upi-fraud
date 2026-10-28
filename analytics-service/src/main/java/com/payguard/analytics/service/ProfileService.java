package com.payguard.analytics.service;

import com.payguard.analytics.domain.PayerProfile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds every payer's fraud profile in memory for the lifetime of the process.
 * See README "known limitations": a restart loses all history (the fix would be Redis).
 */
@Service
public class ProfileService {

    private final ConcurrentHashMap<String, PayerProfile> profiles = new ConcurrentHashMap<>();

    public PayerProfile getOrCreate(String payerVpa) {
        return profiles.computeIfAbsent(payerVpa, key -> new PayerProfile());
    }
}
