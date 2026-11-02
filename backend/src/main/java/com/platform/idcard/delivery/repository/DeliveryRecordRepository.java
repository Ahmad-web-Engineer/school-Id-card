package com.platform.idcard.delivery.repository;

import com.platform.idcard.delivery.entity.DeliveryRecord;
import com.platform.idcard.delivery.entity.DeliveryStatus;
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
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, UUID> {

    Page<DeliveryRecord> findByOrderId(UUID orderId, Pageable pageable);

    List<DeliveryRecord> findByOrderId(UUID orderId);

    Optional<DeliveryRecord> findByIdAndOrderSchoolId(UUID id, UUID schoolId);

    long countByDeliveryNumberStartingWith(String prefix);

    @Query("SELECT d FROM DeliveryRecord d " +
            "WHERE (:schoolId IS NULL OR d.order.school.id = :schoolId) " +
            "AND (:status IS NULL OR d.deliveryStatus = :status)")
    Page<DeliveryRecord> searchAdminDeliveries(
            @Param("schoolId") UUID schoolId,
            @Param("status") DeliveryStatus status,
            Pageable pageable
    );
}
