package com.platform.idcard.production.repository;

import com.platform.idcard.production.entity.ReprintRequest;
import com.platform.idcard.production.entity.ReprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReprintRequestRepository extends JpaRepository<ReprintRequest, UUID> {

    List<ReprintRequest> findAllByAssignmentId(UUID assignmentId);

    boolean existsByRequestNumber(String requestNumber);

    @Query("""
        SELECT rr FROM ReprintRequest rr
        WHERE (:assignmentId IS NULL OR rr.assignment.id = :assignmentId)
          AND (:status IS NULL OR rr.approvalStatus = :status)
        ORDER BY rr.createdAt DESC
    """)
    Page<ReprintRequest> searchReprintRequests(
            @Param("assignmentId") UUID assignmentId,
            @Param("status") ReprintStatus status,
            Pageable pageable
    );
}
