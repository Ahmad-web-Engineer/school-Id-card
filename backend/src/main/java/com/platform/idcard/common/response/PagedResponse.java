package com.platform.idcard.common.response;

import java.util.List;

public record PagedResponse<T>(
        boolean success,
        List<T> data,
        PaginationMeta pagination
) {
    public static <T> PagedResponse<T> of(List<T> data, PaginationMeta pagination) {
        return new PagedResponse<>(true, data, pagination);
    }
}
