package com.payguard.payment.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class StatsResponse {
    private final long checkedCount;
    private final long flaggedCount;
    private final double flagRate;
    private final double averageCheckLatencyMs;
    private final Map<String, Long> countsByRule;
    private final List<ThroughputPoint> throughput;

    public StatsResponse(long checkedCount, long flaggedCount, double flagRate, double averageCheckLatencyMs,
                          Map<String, Long> countsByRule, List<ThroughputPoint> throughput) {
        this.checkedCount = checkedCount;
        this.flaggedCount = flaggedCount;
        this.flagRate = flagRate;
        this.averageCheckLatencyMs = averageCheckLatencyMs;
        this.countsByRule = countsByRule;
        this.throughput = throughput;
    }

    public long getCheckedCount() {
        return checkedCount;
    }

    public long getFlaggedCount() {
        return flaggedCount;
    }

    public double getFlagRate() {
        return flagRate;
    }

    public double getAverageCheckLatencyMs() {
        return averageCheckLatencyMs;
    }

    public Map<String, Long> getCountsByRule() {
        return countsByRule;
    }

    public List<ThroughputPoint> getThroughput() {
        return throughput;
    }

    public static class ThroughputPoint {
        private final Instant intervalStart;
        private final long count;

        public ThroughputPoint(Instant intervalStart, long count) {
            this.intervalStart = intervalStart;
            this.count = count;
        }

        public Instant getIntervalStart() {
            return intervalStart;
        }

        public long getCount() {
            return count;
        }
    }
}
