package com.platform.idcard.common.response;

public record PaginationMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static PaginationMeta of(int page, int size, long totalElements, int totalPages, boolean last) {
        return new PaginationMeta(page, size, totalElements, totalPages, last);
    }
}
