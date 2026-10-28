package com.payguard.analytics.controller;

import com.payguard.analytics.dto.SummaryResponse;
import com.payguard.analytics.service.SummaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsSummaryController {

    private final SummaryService summaryService;

    public AnalyticsSummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping("/summary")
    public SummaryResponse summary() {
        return summaryService.summarizeLastHour();
    }
}
