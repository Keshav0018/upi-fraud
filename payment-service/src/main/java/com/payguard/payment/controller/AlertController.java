package com.payguard.payment.controller;

import com.payguard.payment.domain.AlertStatus;
import com.payguard.payment.dto.AlertResponse;
import com.payguard.payment.dto.DecisionRequest;
import com.payguard.payment.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        return alertService.listAlerts(status);
    }

    @GetMapping("/{id}")
    public AlertResponse get(@PathVariable UUID id) {
        return alertService.getAlert(id);
    }

    @PostMapping("/{id}/decision")
    public AlertResponse decide(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        return alertService.decide(id, request);
    }
}
