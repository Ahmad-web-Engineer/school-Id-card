package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.DefectCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QualityCheckRequest(
        @NotNull(message = "Inspected quantity is required")
        @Min(value = 1, message = "Inspected quantity must be at least 1")
        Integer inspectedQuantity,

        @NotNull(message = "Accepted quantity is required")
        @Min(value = 0, message = "Accepted quantity cannot be negative")
        Integer acceptedQuantity,

        @NotNull(message = "Rejected quantity is required")
        @Min(value = 0, message = "Rejected quantity cannot be negative")
        Integer rejectedQuantity,

        DefectCategory defectCategory,
        String defectDescription,
        String notes
) {}
