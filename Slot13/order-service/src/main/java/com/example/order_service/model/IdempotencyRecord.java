package com.example.order_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;
    private String orderId;
    private String responseStatus;
    private LocalDateTime createdAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String orderId, String responseStatus, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.responseStatus = responseStatus;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
