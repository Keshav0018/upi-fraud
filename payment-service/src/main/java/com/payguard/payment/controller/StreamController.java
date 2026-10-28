package com.payguard.payment.controller;

import com.payguard.payment.sse.SsePublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {

    private final SsePublisher ssePublisher;

    public StreamController(SsePublisher ssePublisher) {
        this.ssePublisher = ssePublisher;
    }

    @GetMapping(path = "/api/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return ssePublisher.subscribe();
    }
}
