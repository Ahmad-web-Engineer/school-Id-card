/**
 * Safely extracts an array from various API response shapes:
 * - Direct array: [...]
 * - Standard PagedResponse: { success: true, data: [...], pagination: {...} }
 * - ApiResponse<PagedResponse>: { success: true, data: { success: true, data: [...], pagination: {...} } }
 * - Spring Data Page: { content: [...], totalPages: ... } or { data: { content: [...] } }
 */
export function extractArray(response) {
  if (!response) return [];
  if (Array.isArray(response)) return response;
  if (Array.isArray(response.data)) return response.data;
  if (Array.isArray(response.data?.data)) return response.data.data;
  if (Array.isArray(response.data?.content)) return response.data.content;
  if (Array.isArray(response.content)) return response.content;
  return [];
}

/**
 * Safely extracts pagination metadata from various API response shapes.
 */
export function extractPagination(response, fallbackCount = 0) {
  if (!response) {
    return { page: 0, size: 10, totalPages: 0, totalElements: 0, last: true };
  }
  const pagination = response.pagination || response.data?.pagination;
  if (pagination) {
    return {
      page: pagination.page ?? 0,
      size: pagination.size ?? 10,
      totalPages: pagination.totalPages ?? (fallbackCount > 0 ? 1 : 0),
      totalElements: pagination.totalElements ?? fallbackCount,
      last: pagination.last ?? true,
    };
  }
  const totalPages = response.totalPages ?? response.data?.totalPages ?? (fallbackCount > 0 ? 1 : 0);
  const totalElements = response.totalElements ?? response.data?.totalElements ?? fallbackCount;
  return {
    page: response.page ?? response.data?.page ?? 0,
    size: response.size ?? response.data?.size ?? 10,
    totalPages,
    totalElements,
    last: totalPages <= 1,
  };
}
