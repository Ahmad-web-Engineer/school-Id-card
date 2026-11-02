package com.platform.idcard.finance.repository;

import com.platform.idcard.finance.entity.AgencySettlement;
import com.platform.idcard.finance.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencySettlementRepository extends JpaRepository<AgencySettlement, UUID> {

    Page<AgencySettlement> findByAgencyId(UUID agencyId, Pageable pageable);

    Optional<AgencySettlement> findByIdAndAgencyId(UUID id, UUID agencyId);

    Optional<AgencySettlement> findByAssignmentId(UUID assignmentId);

    long countBySettlementNumberStartingWith(String prefix);

    @Query("SELECT COALESCE(SUM(s.agreedAmount), 0) FROM AgencySettlement s WHERE s.agency.id = :agencyId")
    BigDecimal sumAgreedAmountByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT COALESCE(SUM(s.deductionAmount), 0) FROM AgencySettlement s WHERE s.agency.id = :agencyId")
    BigDecimal sumDeductionsByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT COALESCE(SUM(s.payableAmount), 0) FROM AgencySettlement s WHERE s.agency.id = :agencyId")
    BigDecimal sumPayableAmountByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM AgencySettlement s WHERE s.agency.id = :agencyId")
    BigDecimal sumPaidAmountByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT COALESCE(SUM(s.outstandingAmount), 0) FROM AgencySettlement s WHERE s.agency.id = :agencyId")
    BigDecimal sumOutstandingAmountByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT COALESCE(SUM(s.payableAmount), 0) FROM AgencySettlement s")
    BigDecimal sumAllPayableAmount();

    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM AgencySettlement s")
    BigDecimal sumAllPaidAmount();

    @Query("SELECT COALESCE(SUM(s.outstandingAmount), 0) FROM AgencySettlement s")
    BigDecimal sumAllOutstandingAmount();

    @Query("SELECT s FROM AgencySettlement s " +
            "WHERE (:agencyId IS NULL OR s.agency.id = :agencyId) " +
            "AND (:status IS NULL OR s.settlementStatus = :status)")
    Page<AgencySettlement> searchAdminSettlements(
            @Param("agencyId") UUID agencyId,
            @Param("status") SettlementStatus status,
            Pageable pageable
    );
}
