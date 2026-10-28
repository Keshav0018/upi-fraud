package com.payguard.payment.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Component
public class SsePublisher {

    private static final Logger log = LoggerFactory.getLogger(SsePublisher.class);
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                log.debug("Dropping dead SSE connection: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
