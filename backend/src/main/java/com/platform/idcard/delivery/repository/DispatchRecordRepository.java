package com.platform.idcard.delivery.repository;

import com.platform.idcard.delivery.entity.DispatchRecord;
import com.platform.idcard.delivery.entity.DispatchStatus;
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
public interface DispatchRecordRepository extends JpaRepository<DispatchRecord, UUID> {

    Optional<DispatchRecord> findByIdAndAssignmentAgencyId(UUID id, UUID agencyId);

    Page<DispatchRecord> findByAssignmentAgencyId(UUID agencyId, Pageable pageable);

    Page<DispatchRecord> findByAssignmentId(UUID assignmentId, Pageable pageable);

    Page<DispatchRecord> findByOrderId(UUID orderId, Pageable pageable);

    List<DispatchRecord> findByOrderId(UUID orderId);

    long countByDispatchNumberStartingWith(String prefix);

    @Query("SELECT d FROM DispatchRecord d " +
            "LEFT JOIN d.assignment a " +
            "WHERE (:agencyId IS NULL OR a.agency.id = :agencyId) " +
            "AND (:status IS NULL OR d.dispatchStatus = :status)")
    Page<DispatchRecord> searchAdminDispatches(
            @Param("agencyId") UUID agencyId,
            @Param("status") DispatchStatus status,
            Pageable pageable
    );
}
