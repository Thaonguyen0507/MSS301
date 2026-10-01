package com.fudn.inventoryservice.repository;

import com.fudn.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

// IS-2: Spring Data JPA repository with derived query for stock check
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Returns true if a record exists with the given skuCode
     * and quantity >= the requested amount.
     *
     * Spring Data generates:
     *   SELECT COUNT(*) > 0 FROM t_inventory
     *   WHERE sku_code = ? AND quantity >= ?
     */
    boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(String skuCode, int quantity);
}
