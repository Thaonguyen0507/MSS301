package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentRefundRequest;
import com.example.payment_service.model.Payment;
import com.example.payment_service.service.PaymentService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment/process")
    public ResponseEntity<Map<String, String>> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("[CorrelationId: {}] Process payment request for order: {}, amount: {}",
                correlationId, request.getOrderId(), request.getAmount());
        paymentService.processPayment(request.getOrderId(), request.getAmount());
        return ResponseEntity.ok(Map.of("message", "Payment processed successfully"));
    }

    @PostMapping("/payment/refund")
    public ResponseEntity<Map<String, String>> refundPayment(
            @RequestBody PaymentRefundRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("[CorrelationId: {}] Refund payment request for order: {}", correlationId, request.getOrderId());
        paymentService.refundPayment(request.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Payment refunded successfully"));
    }

    @GetMapping("/payment/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
}
