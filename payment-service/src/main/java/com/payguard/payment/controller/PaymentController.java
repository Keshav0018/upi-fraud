package com.payguard.payment.controller;

import com.payguard.payment.dto.PaymentRequest;
import com.payguard.payment.dto.PaymentResponse;
import com.payguard.payment.dto.UpdatePaymentNoteRequest;
import com.payguard.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(required = false) String payerVpa) {
        return paymentService.listPayments(payerVpa);
    }

    @PutMapping("/{id}")
    public PaymentResponse updateNote(@PathVariable UUID id, @Valid @RequestBody UpdatePaymentNoteRequest request) {
        return paymentService.updateNote(id, request.getNote());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
