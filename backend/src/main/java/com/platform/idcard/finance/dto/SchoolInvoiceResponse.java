package com.platform.idcard.finance.dto;

import com.platform.idcard.finance.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolInvoiceResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String invoiceNumber,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        InvoiceStatus invoiceStatus,
        OffsetDateTime issuedAt,
        LocalDate dueDate,
        String notes
) {}
