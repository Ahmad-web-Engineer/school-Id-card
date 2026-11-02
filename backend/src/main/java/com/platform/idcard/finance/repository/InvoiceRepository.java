package com.platform.idcard.finance.repository;

import com.platform.idcard.finance.entity.Invoice;
import com.platform.idcard.finance.entity.InvoiceStatus;
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
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findBySchoolId(UUID schoolId, Pageable pageable);

    Optional<Invoice> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<Invoice> findByOrderId(UUID orderId);

    long countByInvoiceNumberStartingWith(String prefix);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.school.id = :schoolId AND i.invoiceStatus != 'CANCELLED'")
    BigDecimal sumTotalInvoicedBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i WHERE i.school.id = :schoolId AND i.invoiceStatus != 'CANCELLED'")
    BigDecimal sumDueAmountBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.invoiceStatus != 'CANCELLED'")
    BigDecimal sumAllInvoicedAmount();

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i WHERE i.invoiceStatus != 'CANCELLED'")
    BigDecimal sumAllDueAmount();

    @Query("SELECT i FROM Invoice i " +
            "WHERE (:schoolId IS NULL OR i.school.id = :schoolId) " +
            "AND (:status IS NULL OR i.invoiceStatus = :status)")
    Page<Invoice> searchAdminInvoices(
            @Param("schoolId") UUID schoolId,
            @Param("status") InvoiceStatus status,
            Pageable pageable
    );
}
