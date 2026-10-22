package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.model.IdempotencyRecord;
import com.example.order_service.model.Order;
import com.example.order_service.model.SagaState;
import com.example.order_service.repository.IdempotencyRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.SagaStateRepository;
import com.example.order_service.saga.SagaOrchestrator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final SagaStateRepository sagaStateRepository;
    private final SagaOrchestrator sagaOrchestrator;

    public OrderService(OrderRepository orderRepository,
                        IdempotencyRepository idempotencyRepository,
                        SagaStateRepository sagaStateRepository,
                        SagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.sagaStateRepository = sagaStateRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public Order createOrder(OrderRequest request, String idempotencyKey, String correlationId) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<IdempotencyRecord> recordOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if (recordOpt.isPresent()) {
                String existingOrderId = recordOpt.get().getOrderId();
                return orderRepository.findById(existingOrderId)
                    .orElseThrow(() -> new OrderNotFoundException("Cached order not found: " + existingOrderId));
            }
        }

        Order order = new Order(
            request.getProductId(),
            request.getQuantity(),
            request.getAmount(),
            "PENDING",
            correlationId
        );
        Order savedOrder = orderRepository.save(order);

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                savedOrder.getId(),
                savedOrder.getStatus(),
                LocalDateTime.now()
            );
            idempotencyRepository.save(record);
        }

        sagaOrchestrator.executeSaga(savedOrder, correlationId);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public SagaState getSagaState(String orderId) {
        return sagaStateRepository.findBySagaId(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Saga state not found for order: " + orderId));
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdempotencyKey(String idempotencyKey) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isPresent()) {
            String existingOrderId = recordOpt.get().getOrderId();
            return orderRepository.findById(existingOrderId)
                .orElseThrow(() -> new OrderNotFoundException("Cached order not found: " + existingOrderId));
        }
        return null;
    }
}
