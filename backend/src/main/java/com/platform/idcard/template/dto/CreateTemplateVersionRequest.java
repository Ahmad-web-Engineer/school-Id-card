package com.platform.idcard.template.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTemplateVersionRequest(
        @NotNull(message = "Card width in mm is required")
        @DecimalMin(value = "10.0", message = "Card width must be at least 10 mm")
        @DecimalMax(value = "300.0", message = "Card width cannot exceed 300 mm")
        BigDecimal cardWidthMm,

        @NotNull(message = "Card height in mm is required")
        @DecimalMin(value = "10.0", message = "Card height must be at least 10 mm")
        @DecimalMax(value = "300.0", message = "Card height cannot exceed 300 mm")
        BigDecimal cardHeightMm,

        @DecimalMin(value = "0.0", message = "Bleed margin cannot be negative")
        @DecimalMax(value = "20.0", message = "Bleed margin cannot exceed 20 mm")
        BigDecimal bleedMarginMm,

        @NotBlank(message = "Front design JSON is required")
        String frontDesignJson,

        @NotBlank(message = "Back design JSON is required")
        String backDesignJson
) {}
