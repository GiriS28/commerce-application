package com.nisum.productservice.dto;

import com.nisum.productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(

        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
}