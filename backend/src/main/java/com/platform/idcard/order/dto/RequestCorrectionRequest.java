package com.platform.idcard.order.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestCorrectionRequest(
        @NotBlank(message = "Correction remarks are required")
        String remarks
) {}
