package com.platform.idcard.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBatchRequest(
        @NotNull(message = "Planned quantity is required")
        @Min(value = 1, message = "Planned quantity must be at least 1")
        Integer plannedQuantity,

        String notes
) {}
