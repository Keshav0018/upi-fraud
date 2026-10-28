package com.payguard.payment.controller;

import com.payguard.payment.dto.StatsResponse;
import com.payguard.payment.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsResponse stats() {
        return statsService.computeStats();
    }
}
