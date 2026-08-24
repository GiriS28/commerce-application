package com.nisum.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated API response")
public record PageResponse<T>(

        @Schema(description = "Records returned for the current page")
        List<T> content,

        @Schema(description = "Zero-based page number", example = "0")
        int page,

        @Schema(description = "Number of records requested per page", example = "20")
        int size,

        @Schema(description = "Total number of matching records", example = "100")
        long totalElements,

        @Schema(description = "Total number of available pages", example = "5")
        int totalPages,

        @Schema(description = "Whether this is the first page")
        boolean first,

        @Schema(description = "Whether this is the last page")
        boolean last
) {
}
