package com.platform.idcard.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.platform.idcard.student.entity.StudentImportBatch;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentImportBatchResponse(
        UUID batchId,
        String fileName,
        String fileType,
        int totalRows,
        int validRows,
        int errorRows,
        Integer importedRows,
        String importStatus,
        List<StudentImportRowResponse> previewRows,
        List<StudentImportRowResponse> rows,
        Instant createdAt,
        Instant completedAt
) {
    public static StudentImportBatchResponse ofUpload(
            StudentImportBatch batch,
            String fileName,
            List<StudentImportRowResponse> previewRows
    ) {
        return new StudentImportBatchResponse(
                batch.getId(),
                fileName,
                batch.getFileType(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getErrorRows(),
                null,
                batch.getImportStatus(),
                previewRows,
                null,
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }

    public static StudentImportBatchResponse ofDetail(
            StudentImportBatch batch,
            String fileName,
            List<StudentImportRowResponse> rows
    ) {
        return new StudentImportBatchResponse(
                batch.getId(),
                fileName,
                batch.getFileType(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getErrorRows(),
                batch.getImportedRows(),
                batch.getImportStatus(),
                null,
                rows,
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }
}
