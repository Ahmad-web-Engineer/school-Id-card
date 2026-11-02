package com.platform.idcard.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Template version ID is required")
        UUID templateVersionId,

        String academicSession,

        String deliveryModel,

        String deliveryAddress,

        String contactPerson,

        String contactMobile,

        String remarks,

        @NotEmpty(message = "At least one student must be selected for the order")
        List<UUID> studentIds,

        @Valid
        List<AdditionalItemRequest> additionalItems
) {}
