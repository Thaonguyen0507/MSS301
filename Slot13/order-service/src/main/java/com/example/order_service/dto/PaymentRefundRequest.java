package com.example.order_service.dto;

public class PaymentRefundRequest {

    private String orderId;

    public PaymentRefundRequest() {
    }

    public PaymentRefundRequest(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
