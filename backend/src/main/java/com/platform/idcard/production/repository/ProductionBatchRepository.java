package com.platform.idcard.production.repository;

import com.platform.idcard.production.entity.ProductionBatch;
import com.platform.idcard.production.entity.ProductionBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, UUID> {

    List<ProductionBatch> findAllByAssignmentId(UUID assignmentId);

    Page<ProductionBatch> findAllByAssignmentId(UUID assignmentId, Pageable pageable);

    boolean existsByBatchNumber(String batchNumber);

    @Query("""
        SELECT pb FROM ProductionBatch pb
        WHERE (:assignmentId IS NULL OR pb.assignment.id = :assignmentId)
          AND (:agencyId IS NULL OR pb.assignment.agency.id = :agencyId)
          AND (:status IS NULL OR pb.productionStatus = :status)
        ORDER BY pb.createdAt DESC
    """)
    Page<ProductionBatch> searchBatches(
            @Param("assignmentId") UUID assignmentId,
            @Param("agencyId") UUID agencyId,
            @Param("status") ProductionBatchStatus status,
            Pageable pageable
    );

    @Query("SELECT pb FROM ProductionBatch pb WHERE pb.id = :batchId AND pb.assignment.agency.id = :agencyId")
    Optional<ProductionBatch> findByIdAndAgencyId(@Param("batchId") UUID batchId, @Param("agencyId") UUID agencyId);
}
