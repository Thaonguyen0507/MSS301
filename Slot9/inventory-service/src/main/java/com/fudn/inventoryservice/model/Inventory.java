package com.fudn.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;

// IS-1: Inventory JPA Entity mapped to t_inventory table
@Entity
@Table(name = "t_inventory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String skuCode;

    private Integer quantity;
}
