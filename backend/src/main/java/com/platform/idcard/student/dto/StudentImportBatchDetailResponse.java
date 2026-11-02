package com.platform.idcard.student.dto;

import com.platform.idcard.common.response.PaginationMeta;

public record StudentImportBatchDetailResponse(
        boolean success,
        StudentImportBatchResponse data,
        PaginationMeta pagination
) {
    public static StudentImportBatchDetailResponse of(StudentImportBatchResponse data, PaginationMeta pagination) {
        return new StudentImportBatchDetailResponse(true, data, pagination);
    }
}
