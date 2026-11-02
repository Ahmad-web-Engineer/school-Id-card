package com.platform.idcard.student.dto;

import java.util.List;
import java.util.UUID;

public record PhotoImportBatchResponse(
        UUID batchId,
        int totalFiles,
        int matchedCount,
        int unmatchedCount,
        int duplicateCount,
        String importStatus,
        List<PhotoImportResultResponse> results
) {}
