package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.saga.SagaOrchestrator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaOrchestrator sagaOrchestrator;

    public OrderService(OrderRepository orderRepository,
                        SagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public Order createOrder(OrderRequest request, String correlationId) {
        Order order = new Order(
            request.getProductId(),
            request.getQuantity(),
            request.getAmount(),
            "PENDING",
            correlationId
        );
        Order savedOrder = orderRepository.save(order);

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

}
