package com.platform.idcard.production.repository;

import com.platform.idcard.production.entity.QualityCheck;
import com.platform.idcard.production.entity.QualityCheckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheck, UUID> {

    List<QualityCheck> findAllByAssignmentId(UUID assignmentId);

    Page<QualityCheck> findAllByAssignmentId(UUID assignmentId, Pageable pageable);

    boolean existsByCheckNumber(String checkNumber);

    @Query("""
        SELECT qc FROM QualityCheck qc
        WHERE (:assignmentId IS NULL OR qc.assignment.id = :assignmentId)
          AND (:agencyId IS NULL OR qc.assignment.agency.id = :agencyId)
          AND (:status IS NULL OR qc.inspectionStatus = :status)
        ORDER BY qc.inspectedAt DESC
    """)
    Page<QualityCheck> searchQualityChecks(
            @Param("assignmentId") UUID assignmentId,
            @Param("agencyId") UUID agencyId,
            @Param("status") QualityCheckStatus status,
            Pageable pageable
    );
}
