package com.example.order_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_state")
public class SagaState {

    @Id
    private String sagaId;
    private String currentStep;
    private String status;

    @Column(length = 1000)
    private String failureReason;
    private LocalDateTime updatedAt;

    public SagaState() {
    }

    public SagaState(String sagaId, String currentStep, String status, String failureReason, LocalDateTime updatedAt) {
        this.sagaId = sagaId;
        this.currentStep = currentStep;
        this.status = status;
        this.failureReason = failureReason;
        this.updatedAt = updatedAt;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
