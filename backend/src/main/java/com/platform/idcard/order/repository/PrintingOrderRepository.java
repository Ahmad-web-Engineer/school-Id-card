package com.platform.idcard.order.repository;

import com.platform.idcard.order.entity.PrintingOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrintingOrderRepository extends JpaRepository<PrintingOrder, UUID> {

    Optional<PrintingOrder> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByOrderNumber(String orderNumber);

    @Query("""
            SELECT o FROM PrintingOrder o
            WHERE (:schoolId IS NULL OR o.school.id = :schoolId)
              AND (:status IS NULL OR o.orderStatus = :status)
              AND (:search IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(o.contactPerson) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(o.school.schoolName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<PrintingOrder> findWithFilters(
            @Param("schoolId") UUID schoolId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM PrintingOrder o WHERE o.school.id = :schoolId")
    long countBySchoolId(@Param("schoolId") UUID schoolId);

    long countByOrderStatus(String orderStatus);

    long countBySchoolIdAndOrderStatus(UUID schoolId, String orderStatus);

    @Query("SELECT COUNT(o) FROM PrintingOrder o WHERE o.orderStatus IN :statuses")
    long countByOrderStatusIn(@Param("statuses") java.util.Collection<String> statuses);

    @Query("SELECT COUNT(o) FROM PrintingOrder o WHERE o.school.id = :schoolId AND o.orderStatus IN :statuses")
    long countBySchoolIdAndOrderStatusIn(@Param("schoolId") UUID schoolId, @Param("statuses") java.util.Collection<String> statuses);

    @Query("SELECT o.orderStatus, COUNT(o) FROM PrintingOrder o GROUP BY o.orderStatus")
    java.util.List<Object[]> countGroupByOrderStatus();
}
