package com.platform.idcard.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HubReceiveRequest(
        @NotNull(message = "Received quantity is required")
        @Min(value = 1, message = "Received quantity must be at least 1")
        Integer receivedQuantity,

        String conditionStatus,

        String hubNotes
) {}
