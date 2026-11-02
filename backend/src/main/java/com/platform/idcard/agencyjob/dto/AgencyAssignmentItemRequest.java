package com.platform.idcard.agencyjob.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AgencyAssignmentItemRequest(
        @NotNull(message = "Agency ID is required")
        UUID agencyId,

        LocalDate expectedDeliveryDate,

        @NotNull(message = "Agreed purchase rate is required")
        @DecimalMin(value = "0.00", message = "Agreed purchase rate must be non-negative")
        BigDecimal agreedRate,

        String notes,

        @NotEmpty(message = "At least one job item must be allocated")
        @Valid
        List<JobItemAllocationRequest> jobItems
) {}
