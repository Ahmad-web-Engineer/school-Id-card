package com.platform.idcard.finance.repository;

import com.platform.idcard.finance.entity.Payment;
import com.platform.idcard.finance.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findBySchoolId(UUID schoolId, Pageable pageable);

    Page<Payment> findByOrderId(UUID orderId, Pageable pageable);

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByIdAndSchoolId(UUID id, UUID schoolId);

    long countByPaymentReferenceStartingWith(String prefix);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school.id = :schoolId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.id = :orderId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'COMPLETED'")
    BigDecimal sumAllCompletedPayments();

    @Query("SELECT p FROM Payment p " +
            "WHERE (:schoolId IS NULL OR p.school.id = :schoolId) " +
            "AND (:status IS NULL OR p.paymentStatus = :status)")
    Page<Payment> searchAdminPayments(
            @Param("schoolId") UUID schoolId,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
