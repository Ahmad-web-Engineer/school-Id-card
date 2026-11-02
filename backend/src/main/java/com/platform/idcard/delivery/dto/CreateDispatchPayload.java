package com.platform.idcard.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDispatchPayload(
        @NotBlank(message = "Courier name is required")
        String courierName,

        @NotBlank(message = "Tracking number is required")
        String trackingNumber,

        @NotNull(message = "Dispatch quantity is required")
        @Min(value = 1, message = "Dispatch quantity must be at least 1")
        Integer dispatchQuantity,

        String dispatchNotes,

        UUID proofFileId
) {}
