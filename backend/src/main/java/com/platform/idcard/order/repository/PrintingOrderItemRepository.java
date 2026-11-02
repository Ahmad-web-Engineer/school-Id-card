package com.platform.idcard.order.repository;

import com.platform.idcard.order.entity.PrintingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrintingOrderItemRepository extends JpaRepository<PrintingOrderItem, UUID> {
    List<PrintingOrderItem> findByOrderId(UUID orderId);
}
