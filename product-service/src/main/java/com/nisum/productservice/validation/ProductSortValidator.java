package com.nisum.productservice.validation;

import com.nisum.productservice.dto.ProductFilterRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProductSortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "price",
            "category",
            "createdAt",
            "updatedAt"
    );

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

    public void validate(Pageable pageable) {

        pageable.getSort().forEach(order -> {

            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Sorting by '" + order.getProperty() + "' is not supported"
                );
            }
        });
    }
}