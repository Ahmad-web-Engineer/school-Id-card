package com.platform.idcard.finance.dto;

import java.time.LocalDate;

public record GenerateInvoiceRequest(
        LocalDate dueDate,
        String notes
) {}
