package com.platform.idcard.agencyjob.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JobItemAllocationRequest(
        @NotNull(message = "Order item ID is required")
        UUID orderItemId,

        @NotNull(message = "Assigned quantity is required")
        @Min(value = 1, message = "Assigned quantity must be at least 1")
        Integer assignedQuantity
) {}
