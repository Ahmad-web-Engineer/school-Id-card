package com.platform.idcard.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePlatformSettingsRequest(
        @NotNull(message = "PVC card price is required")
        @DecimalMin(value = "0.00", message = "PVC card price cannot be negative")
        BigDecimal pricingPvcCard,

        @NotNull(message = "RFID card price is required")
        @DecimalMin(value = "0.00", message = "RFID card price cannot be negative")
        BigDecimal pricingRfidCard,

        @NotNull(message = "Lanyard price is required")
        @DecimalMin(value = "0.00", message = "Lanyard price cannot be negative")
        BigDecimal pricingLanyard,

        @NotNull(message = "Card holder price is required")
        @DecimalMin(value = "0.00", message = "Card holder price cannot be negative")
        BigDecimal pricingCardHolder,

        @NotNull(message = "Rush fee is required")
        @DecimalMin(value = "0.00", message = "Rush fee cannot be negative")
        BigDecimal pricingRushFee,

        @NotNull(message = "Agency cost per card is required")
        @DecimalMin(value = "0.00", message = "Agency cost per card cannot be negative")
        BigDecimal agencyCostPerCard,

        @NotNull(message = "Tax rate percentage is required")
        @DecimalMin(value = "0.00", message = "Tax rate percentage cannot be negative")
        BigDecimal taxRatePercent,

        @NotNull(message = "Flat shipping fee is required")
        @DecimalMin(value = "0.00", message = "Flat shipping fee cannot be negative")
        BigDecimal flatShippingFee,

        @NotNull(message = "Minimum order quantity is required")
        @Min(value = 1, message = "Minimum order quantity must be at least 1")
        Integer minOrderQuantity,

        @NotNull(message = "Payment due days is required")
        @Min(value = 1, message = "Payment due days must be at least 1")
        Integer paymentDueDays,

        @NotNull(message = "Quotation validity days is required")
        @Min(value = 1, message = "Quotation validity days must be at least 1")
        Integer quotationValidityDays,

        @NotBlank(message = "Default delivery model is required")
        String defaultDeliveryModel,

        @NotBlank(message = "Platform currency is required")
        String platformCurrency
) {}
