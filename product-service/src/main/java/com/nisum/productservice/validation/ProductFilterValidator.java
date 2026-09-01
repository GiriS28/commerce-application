package com.nisum.productservice.validation;

import com.nisum.productservice.dto.ProductFilterRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductFilterValidator {

    public void validate(ProductFilterRequest filter) {

        if (filter.minPrice() != null &&
                filter.minPrice().signum() < 0) {

            throw new IllegalArgumentException(
                    "minPrice cannot be negative"
            );
        }

        if (filter.maxPrice() != null &&
                filter.maxPrice().signum() < 0) {

            throw new IllegalArgumentException(
                    "maxPrice cannot be negative"
            );
        }

        if (filter.minPrice() != null &&
                filter.maxPrice() != null &&
                filter.minPrice().compareTo(filter.maxPrice()) > 0) {

            throw new IllegalArgumentException(
                    "minPrice cannot be greater than maxPrice"
            );
        }
    }
}
