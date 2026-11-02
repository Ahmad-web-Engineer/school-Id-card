package com.platform.idcard.finance.dto;

import com.platform.idcard.finance.entity.SettlementStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgencySettlementResponse(
        UUID id,
        UUID assignmentId,
        String jobNumber,
        String settlementNumber,
        BigDecimal agreedAmount,
        BigDecimal deductionAmount,
        BigDecimal penaltyAmount,
        BigDecimal payableAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        SettlementStatus settlementStatus,
        String paymentReference,
        OffsetDateTime settledAt,
        String notes
) {}
