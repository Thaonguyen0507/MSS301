package com.example.order_service.controller;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.model.SagaState;
import com.example.order_service.service.OrderService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        log.info("[CorrelationId: {}] Received create order request for product: {}", correlationId, request.getProductId());

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            try {
                Order cachedOrder = orderService.getOrderByIdempotencyKey(idempotencyKey);
                if (cachedOrder != null) {
                    log.info("[CorrelationId: {}] Returning cached order for idempotency key: {}", correlationId, idempotencyKey);
                    return ResponseEntity.ok()
                            .header("X-Idempotent-Response", "true")
                            .header("X-Correlation-ID", correlationId)
                            .body(cachedOrder);
                }
            } catch (Exception ignored) {
            }
        }

        Order order = orderService.createOrder(request, idempotencyKey, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/saga/{orderId}")
    public ResponseEntity<SagaState> getSagaState(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getSagaState(orderId));
    }
}
