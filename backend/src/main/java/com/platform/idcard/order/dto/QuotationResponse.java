package com.platform.idcard.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        String quotationNumber,
        UUID orderId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String quotationStatus,
        LocalDate validUntil,
        Instant approvedAt,
        String notes,
        Instant createdAt,
        List<QuotationItemResponse> items
) {}
