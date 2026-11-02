package com.platform.idcard.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmDeliveryRequest(
        @NotNull(message = "Received quantity is required")
        @Min(value = 1, message = "Received quantity must be at least 1")
        Integer receivedQuantity,

        @NotBlank(message = "Received by person name is required")
        String receivedBy,

        String receivedMobile,

        String deliveryNotes,

        UUID proofOfDeliveryFileId
) {}
