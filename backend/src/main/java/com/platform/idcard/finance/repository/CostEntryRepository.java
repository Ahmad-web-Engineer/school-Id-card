package com.platform.idcard.finance.repository;

import com.platform.idcard.finance.entity.CostEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CostEntryRepository extends JpaRepository<CostEntry, UUID> {

    List<CostEntry> findByOrderId(UUID orderId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostEntry c WHERE c.order.id = :orderId")
    BigDecimal sumCostByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostEntry c")
    BigDecimal sumAllCosts();
}
