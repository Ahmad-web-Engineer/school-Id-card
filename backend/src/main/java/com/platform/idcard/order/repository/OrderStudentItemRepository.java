package com.platform.idcard.order.repository;

import com.platform.idcard.order.entity.OrderStudentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStudentItemRepository extends JpaRepository<OrderStudentItem, UUID> {
    List<OrderStudentItem> findByOrderId(UUID orderId);
    Page<OrderStudentItem> findByOrderId(UUID orderId, Pageable pageable);
    long countByOrderId(UUID orderId);
    boolean existsByOrderIdAndStudentId(UUID orderId, UUID studentId);
}
