package com.fudn.order_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_orders", schema = "order_service")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "sku_code")
    private String skuCode;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity")
    private Integer quantity;

}