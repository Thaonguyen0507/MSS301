package com.example.inventory_service.service;

import com.example.inventory_service.exception.InsufficientInventoryException;
import com.example.inventory_service.exception.InventoryNotFoundException;
import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void reserveInventory(String productId, int quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        if (inventory.getQuantity() < quantity) {
            throw new InsufficientInventoryException("Insufficient inventory for product: " + productId);
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedFor(orderId);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void releaseInventory(String productId, int quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        if (orderId.equals(inventory.getReservedFor())) {
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventory.setReservedFor(null);
            inventoryRepository.save(inventory);
        }
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
    }
}
