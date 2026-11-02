package com.platform.idcard.production.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectReprintRequest(
        @NotBlank(message = "Rejection reason is required")
        String rejectionReason
) {}
