package com.platform.audit.api.dto;

/**
 * Standard pagination request parameters.
 *
 * <p>This record encapsulates pagination parameters used across the platform,
 * ensuring consistent pagination behavior and validation.
 *
 * @param page zero-based page number
 * @param size number of items per page (1-100)
 *
 * @since 1.0.0
 */
public record PaginationRequest(int page, int size) {

    /**
     * Default pagination with reasonable defaults.
     */
    public static final PaginationRequest DEFAULT = new PaginationRequest(0, 50);

    /**
     * Maximum allowed page size to prevent performance issues.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Creates a pagination request with validation.
     *
     * @param page the page number (must be >= 0)
     * @param size the page size (must be 1-100)
     */
    public PaginationRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Creates a pagination request with default page size.
     *
     * @param page the page number
     * @return pagination request with default size
     */
    public static PaginationRequest ofPage(int page) {
        return new PaginationRequest(page, DEFAULT.size());
    }

    /**
     * Creates a pagination request with default page number.
     *
     * @param size the page size
     * @return pagination request with default page
     */
    public static PaginationRequest ofSize(int size) {
        return new PaginationRequest(DEFAULT.page(), size);
    }

    /**
     * Calculates the offset for database queries.
     *
     * @return the offset (page * size)
     */
    public int offset() {
        return page * size;
    }
}