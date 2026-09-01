package com.nisum.productservice.dto;

import com.nisum.productservice.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String name,
        String category,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
