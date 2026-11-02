package com.platform.idcard.student.dto;

import java.util.UUID;

public record StudentImportConfirmResponse(
        UUID batchId,
        int totalRows,
        int importedCount,
        int errorCount,
        String importStatus,
        String message
) {}
