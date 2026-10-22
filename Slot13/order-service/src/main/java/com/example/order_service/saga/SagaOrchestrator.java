package com.example.order_service.saga;

import com.example.order_service.dto.InventoryRequest;
import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentRefundRequest;
import com.example.order_service.model.Order;
import com.example.order_service.model.SagaState;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.SagaStateRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private SagaOrchestrator self;

    @Value("${external-services.inventory-url}")
    private String inventoryServiceUrl;

    @Value("${external-services.payment-url}")
    private String paymentServiceUrl;

    public SagaOrchestrator(RestTemplate restTemplate,
                            OrderRepository orderRepository,
                            SagaStateRepository sagaStateRepository) {
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
        this.sagaStateRepository = sagaStateRepository;
    }

    @Autowired
    public void setSelf(@Lazy SagaOrchestrator self) {
        this.self = self;
    }

    public void executeSaga(Order order, String correlationId) {
        String orderId = order.getId();
        log.info("[CorrelationId: {}] [OrderId: {}] Starting saga orchestration", correlationId, orderId);

        SagaState sagaState = new SagaState(orderId, "INIT", "EXECUTING", null, LocalDateTime.now());
        sagaStateRepository.save(sagaState);

        try {
            log.info("[CorrelationId: {}] [OrderId: {}] [Step: INIT] Attempting to reserve inventory", correlationId, orderId);
            self.reserveInventory(order, correlationId);

            sagaState.setCurrentStep("INVENTORY_RESERVED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            log.info("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Inventory reserved successfully", correlationId, orderId);
        } catch (Exception ex) {
            log.error("[CorrelationId: {}] [OrderId: {}] [Step: INIT] Inventory reservation failed: {}", correlationId, orderId, ex.getMessage(), ex);

            sagaState.setStatus("FAILED");
            sagaState.setFailureReason(ex.getMessage());
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus("FAILED");
            orderRepository.save(order);
            return;
        }

        try {
            log.info("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Attempting to process payment", correlationId, orderId);
            self.processPayment(order, correlationId);

            sagaState.setCurrentStep("PAYMENT_DONE");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            log.info("[CorrelationId: {}] [OrderId: {}] [Step: PAYMENT_DONE] Payment processed successfully", correlationId, orderId);
        } catch (Exception ex) {
            log.error("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Payment processing failed: {}", correlationId, orderId, ex.getMessage(), ex);

            sagaState.setStatus("COMPENSATING");
            sagaState.setFailureReason(ex.getMessage());
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            log.info("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Starting compensation: releasing inventory", correlationId, orderId);
            try {
                self.releaseInventory(order, correlationId);
                log.info("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Compensation completed successfully: inventory released", correlationId, orderId);
            } catch (Exception compEx) {
                log.error("[CorrelationId: {}] [OrderId: {}] [Step: INVENTORY_RESERVED] Compensation failed: inventory release error: {}", correlationId, orderId, compEx.getMessage(), compEx);
            }

            sagaState.setStatus("COMPENSATED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            order.setStatus("COMPENSATED");
            orderRepository.save(order);
            return;
        }

        sagaState.setStatus("COMPLETED");
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);

        order.setStatus("COMPLETED");
        orderRepository.save(order);
        log.info("[CorrelationId: {}] [OrderId: {}] [Step: PAYMENT_DONE] Saga completed successfully", correlationId, orderId);
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void reserveInventory(Order order, String correlationId) {
        log.info("[CorrelationId: {}] [OrderId: {}] Sending HTTP reserve inventory request", correlationId, order.getId());
        String url = inventoryServiceUrl + "/inventory/reserve";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        InventoryRequest request = new InventoryRequest(order.getProductId(), order.getQuantity(), order.getId());
        HttpEntity<InventoryRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void releaseInventory(Order order, String correlationId) {
        log.info("[CorrelationId: {}] [OrderId: {}] Sending HTTP release inventory request", correlationId, order.getId());
        String url = inventoryServiceUrl + "/inventory/release";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        InventoryRequest request = new InventoryRequest(order.getProductId(), order.getQuantity(), order.getId());
        HttpEntity<InventoryRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void processPayment(Order order, String correlationId) {
        log.info("[CorrelationId: {}] [OrderId: {}] Sending HTTP process payment request", correlationId, order.getId());
        String url = paymentServiceUrl + "/payment/process";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PaymentRequest request = new PaymentRequest(order.getId(), order.getAmount());
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void refundPayment(Order order, String correlationId) {
        log.info("[CorrelationId: {}] [OrderId: {}] Sending HTTP refund payment request", correlationId, order.getId());
        String url = paymentServiceUrl + "/payment/refund";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PaymentRefundRequest request = new PaymentRefundRequest(order.getId());
        HttpEntity<PaymentRefundRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

}
