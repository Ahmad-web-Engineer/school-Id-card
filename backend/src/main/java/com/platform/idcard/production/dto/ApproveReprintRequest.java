package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.CostBearer;
import jakarta.validation.constraints.NotNull;

public record ApproveReprintRequest(
        @NotNull(message = "Cost bearer is required")
        CostBearer costBearer,

        String notes
) {}
