package com.payguard.analytics.ai;

import com.payguard.analytics.config.AiProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dispatches at most {@code payguard.ai.rate-limit-per-minute} tasks per minute, queueing the rest.
 * LLM calls go through here so a burst of HELD payments doesn't blow through a free-tier API quota.
 */
@Component
public class RateLimitedExecutor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedExecutor.class);

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService dispatcher;
    private final ExecutorService workers;

    public RateLimitedExecutor(AiProperties properties) {
        long periodMs = Math.max(50L, 60_000L / Math.max(1, properties.getRateLimitPerMinute()));
        this.dispatcher = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("ai-rate-limiter"));
        this.workers = Executors.newFixedThreadPool(4, daemonThreadFactory("ai-worker"));
        this.dispatcher.scheduleAtFixedRate(this::dispatchNext, periodMs, periodMs, TimeUnit.MILLISECONDS);
        log.info("AI rate limiter started: ~{} calls/min (one every {}ms)", properties.getRateLimitPerMinute(), periodMs);
    }

    public void submit(Runnable task) {
        queue.add(task);
    }

    private void dispatchNext() {
        Runnable task = queue.poll();
        if (task != null) {
            workers.execute(task);
        }
    }

    @PreDestroy
    void shutdown() {
        dispatcher.shutdownNow();
        workers.shutdownNow();
    }

    private static java.util.concurrent.ThreadFactory daemonThreadFactory(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }
}
