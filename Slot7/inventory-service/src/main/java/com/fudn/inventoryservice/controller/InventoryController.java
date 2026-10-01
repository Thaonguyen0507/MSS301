package com.fudn.inventoryservice.controller;

import com.fudn.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // IS-4: GET /api/inventory?skuCode=iphone_15&quantity=10
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@RequestParam String skuCode,
                             @RequestParam Integer quantity) {
        return inventoryService.isInStock(skuCode, quantity);
    }
}
