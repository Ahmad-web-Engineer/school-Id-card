package com.platform.idcard.agencyjob.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReassignJobRequest(
        @NotNull(message = "New agency ID is required")
        UUID newAgencyId,

        LocalDate expectedDeliveryDate,

        @NotNull(message = "Agreed purchase rate is required")
        @DecimalMin(value = "0.00", message = "Agreed rate must be non-negative")
        BigDecimal agreedRate,

        @NotBlank(message = "Reassignment reason is required")
        String reassignmentReason
) {}
