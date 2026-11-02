package com.platform.idcard.order.repository;

import com.platform.idcard.order.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {
    Optional<Quotation> findByOrderId(UUID orderId);
    Optional<Quotation> findByQuotationNumber(String quotationNumber);
    boolean existsByQuotationNumber(String quotationNumber);
}
