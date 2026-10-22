package com.example.inventory_service.controller;

import com.example.inventory_service.dto.InventoryRequest;
import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.service.InventoryService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inventory/reserve")
    public ResponseEntity<Map<String, String>> reserveInventory(
            @RequestBody InventoryRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("[CorrelationId: {}] Reserve inventory request for product: {}, quantity: {}, orderId: {}",
                correlationId, request.getProductId(), request.getQuantity(), request.getOrderId());
        inventoryService.reserveInventory(request.getProductId(), request.getQuantity(), request.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Inventory reserved successfully"));
    }

    @PostMapping("/inventory/release")
    public ResponseEntity<Map<String, String>> releaseInventory(
            @RequestBody InventoryRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("[CorrelationId: {}] Release inventory request for product: {}, quantity: {}, orderId: {}",
                correlationId, request.getProductId(), request.getQuantity(), request.getOrderId());
        inventoryService.releaseInventory(request.getProductId(), request.getQuantity(), request.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Inventory released successfully"));
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }
}
