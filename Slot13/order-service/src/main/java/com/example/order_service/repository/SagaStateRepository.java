package com.example.order_service.repository;

import com.example.order_service.model.SagaState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {
    Optional<SagaState> findBySagaId(String sagaId);
}
