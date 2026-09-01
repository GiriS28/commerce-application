package com.nisum.productservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        String name,

        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal price,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        @NotNull
        @PositiveOrZero
        Long version
) {
}