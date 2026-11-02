package com.platform.idcard.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductionUpdateRequest(
        @NotNull(message = "Printed quantity is required")
        @Min(value = 0, message = "Printed quantity cannot be negative")
        Integer printedQuantity,

        @NotNull(message = "Rejected quantity is required")
        @Min(value = 0, message = "Rejected quantity cannot be negative")
        Integer rejectedQuantity,

        String remarks,
        String evidenceFileId
) {}
