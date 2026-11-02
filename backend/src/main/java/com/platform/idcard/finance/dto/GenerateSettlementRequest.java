package com.platform.idcard.finance.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record GenerateSettlementRequest(
        @DecimalMin(value = "0.00", message = "Deduction amount cannot be negative")
        BigDecimal deductionAmount,

        @DecimalMin(value = "0.00", message = "Penalty amount cannot be negative")
        BigDecimal penaltyAmount,

        String notes
) {}
