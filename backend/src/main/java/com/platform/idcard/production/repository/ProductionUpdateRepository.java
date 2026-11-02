package com.platform.idcard.production.repository;

import com.platform.idcard.production.entity.ProductionUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionUpdateRepository extends JpaRepository<ProductionUpdate, UUID> {
    List<ProductionUpdate> findAllByBatchIdOrderByCreatedAtDesc(UUID batchId);
}
