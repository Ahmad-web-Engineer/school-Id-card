package com.platform.idcard.agencyjob.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectJobRequest(
        @NotBlank(message = "Rejection reason is required")
        String rejectionReason
) {}
