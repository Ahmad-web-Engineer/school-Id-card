package com.platform.idcard.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record GenerateQuotationRequest(
        @DecimalMin(value = "0.00", message = "Discount cannot be negative")
        BigDecimal discount,

        @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
        BigDecimal taxRate,

        Integer validDays,

        String notes,

        @NotEmpty(message = "Quotation must have at least one line item")
        @Valid
        List<QuotationItemRequest> items
) {}
