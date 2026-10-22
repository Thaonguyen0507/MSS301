package com.example.order_service.repository;

import com.example.order_service.model.IdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
