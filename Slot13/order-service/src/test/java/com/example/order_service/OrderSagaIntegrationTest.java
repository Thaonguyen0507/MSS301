package com.example.order_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.model.SagaState;
import com.example.order_service.repository.IdempotencyRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.SagaStateRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderSagaIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        orderRepository.deleteAll();
        sagaStateRepository.deleteAll();
        idempotencyRepository.deleteAll();
    }

    @Test
    void testSuccessfulOrderSagaFlow() {
        mockServer.expect(requestTo("http://localhost:8081/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("{\"message\":\"Inventory reserved successfully\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8082/payment/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("{\"message\":\"Payment processed successfully\"}", MediaType.APPLICATION_JSON));

        OrderRequest request = new OrderRequest("PROD-001", 2, 200.0);
        ResponseEntity<Order> response = testRestTemplate.postForEntity("/orders", request, Order.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        String orderId = response.getBody().getId();
        assertNotNull(orderId);

        mockServer.verify();

        Order order = orderRepository.findById(orderId).orElse(null);
        assertNotNull(order);
        assertEquals("COMPLETED", order.getStatus());

        SagaState sagaState = sagaStateRepository.findBySagaId(orderId).orElse(null);
        assertNotNull(sagaState);
        assertEquals("COMPLETED", sagaState.getStatus());
        assertEquals("PAYMENT_DONE", sagaState.getCurrentStep());
    }

    @Test
    void testFailedInventorySagaFlow() {
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:8081/inventory/reserve"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                    .andRespond(withServerError());
        }

        OrderRequest request = new OrderRequest("PROD-001", 2, 200.0);
        ResponseEntity<Order> response = testRestTemplate.postForEntity("/orders", request, Order.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        String orderId = response.getBody().getId();

        mockServer.verify();

        Order order = orderRepository.findById(orderId).orElse(null);
        assertNotNull(order);
        assertEquals("FAILED", order.getStatus());

        SagaState sagaState = sagaStateRepository.findBySagaId(orderId).orElse(null);
        assertNotNull(sagaState);
        assertEquals("FAILED", sagaState.getStatus());
        assertEquals("INIT", sagaState.getCurrentStep());
    }

    @Test
    void testFailedPaymentWithCompensationSagaFlow() {
        mockServer.expect(requestTo("http://localhost:8081/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("{\"message\":\"Inventory reserved successfully\"}", MediaType.APPLICATION_JSON));

        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:8082/payment/process"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                    .andRespond(withServerError());
        }

        mockServer.expect(requestTo("http://localhost:8081/inventory/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-ID", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("{\"message\":\"Inventory released successfully\"}", MediaType.APPLICATION_JSON));

        OrderRequest request = new OrderRequest("PROD-001", 2, 200.0);
        ResponseEntity<Order> response = testRestTemplate.postForEntity("/orders", request, Order.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        String orderId = response.getBody().getId();

        mockServer.verify();

        Order order = orderRepository.findById(orderId).orElse(null);
        assertNotNull(order);
        assertEquals("COMPENSATED", order.getStatus());

        SagaState sagaState = sagaStateRepository.findBySagaId(orderId).orElse(null);
        assertNotNull(sagaState);
        assertEquals("COMPENSATED", sagaState.getStatus());
        assertEquals("INVENTORY_RESERVED", sagaState.getCurrentStep());
    }

    @Test
    void testIdempotentOrderCreationFlow() {
        mockServer.expect(requestTo("http://localhost:8081/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"message\":\"Inventory reserved successfully\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8082/payment/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"message\":\"Payment processed successfully\"}", MediaType.APPLICATION_JSON));

        String idempotencyKey = UUID.randomUUID().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        OrderRequest request = new OrderRequest("PROD-001", 2, 200.0);
        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Order> firstResponse = testRestTemplate.exchange("/orders", HttpMethod.POST, entity, Order.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        assertNotNull(firstResponse.getBody());
        String orderId = firstResponse.getBody().getId();
        assertNull(firstResponse.getHeaders().getFirst("X-Idempotent-Response"));

        ResponseEntity<Order> secondResponse = testRestTemplate.exchange("/orders", HttpMethod.POST, entity, Order.class);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        assertNotNull(secondResponse.getBody());
        assertEquals(orderId, secondResponse.getBody().getId());
        assertEquals("true", secondResponse.getHeaders().getFirst("X-Idempotent-Response"));
    }
}
