package com.platform.idcard.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SchoolOrderSummaryResponse(
        UUID id,
        String orderNumber,
        String academicSession,
        String templateName,
        Integer templateVersionNumber,
        Integer totalStudents,
        Integer totalQuantity,
        String orderStatus,
        BigDecimal quotationAmount,
        Instant submittedAt,
        Instant createdAt
) {}
