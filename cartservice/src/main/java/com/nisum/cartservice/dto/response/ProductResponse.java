package com.nisum.cartservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}