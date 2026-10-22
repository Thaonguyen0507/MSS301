package com.example.payment_service.service;

import com.example.payment_service.exception.PaymentDeclinedException;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void processPayment(String orderId, double amount) {
        if (amount > 1000) {
            throw new PaymentDeclinedException("Payment declined: amount exceeds threshold");
        }
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            return;
        }
        Payment payment = new Payment(orderId, amount, "SUCCESS", LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Transactional
    public void refundPayment(String orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (!"REFUNDED".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
            }
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
    }
}
