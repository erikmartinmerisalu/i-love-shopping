package com.lampify.controller;

import com.lampify.dto.ApiErrorResponse;
import com.lampify.dto.PaymentIntentResponse;
import com.lampify.dto.PaymentResultResponse;
import com.lampify.dto.SandboxPaymentRequest;
import com.lampify.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public ResponseEntity<?> createIntent(@RequestBody Map<String, String> body) {
        try {
            String orderNumber = body != null ? body.get("orderNumber") : null;
            if (orderNumber == null || orderNumber.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponse(false, "orderNumber is required"));
            }
            PaymentIntentResponse intent = paymentService.createPaymentIntent(orderNumber.trim());
            return ResponseEntity.ok(intent);
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PostMapping("/sandbox/confirm")
    public ResponseEntity<?> confirmSandbox(@Valid @RequestBody SandboxPaymentRequest request) {
        try {
            PaymentResultResponse result = paymentService.confirmSandboxPayment(request);
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PostMapping("/stripe/sync")
    public ResponseEntity<?> syncStripe(@RequestBody Map<String, String> body) {
        try {
            String orderNumber = body != null ? body.get("orderNumber") : null;
            if (orderNumber == null || orderNumber.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponse(false, "orderNumber is required"));
            }
            return ResponseEntity.ok(paymentService.syncStripePayment(orderNumber.trim()));
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        try {
            paymentService.handleStripeWebhook(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getReason() != null ? ex.getReason() : "webhook error");
        }
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(Map.of(
                "stripeEnabled", paymentService.isStripeConfigured()
        ));
    }

    private ResponseEntity<ApiErrorResponse> error(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(false, ex.getReason() != null ? ex.getReason() : "Payment error"));
    }
}
