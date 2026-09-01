package com.nisum.productservice.specification;

import com.nisum.productservice.dto.ProductFilterRequest;
import com.nisum.productservice.entity.Product;
import com.nisum.productservice.service.ProductServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProductSpecificationBuilder {

    private static final Logger log =
            LoggerFactory.getLogger(ProductServiceImpl.class);

    public Specification<Product> build(ProductFilterRequest filterRequest) {

        log.debug(
                "Building product specification with filters: {}",
                filterRequest
        );
        Specification<Product> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (filterRequest.category() != null &&
                !filterRequest.category().isBlank()) {

            specification = specification.and(
                    ProductSpecification.hasCategory(
                            filterRequest.category()
                    )
            );
        }

        if (filterRequest.status() != null) {

            specification = specification.and(
                    ProductSpecification.hasStatus(
                            filterRequest.status()
                    )
            );
        }

        if (filterRequest.minPrice() != null) {

            specification = specification.and(
                    ProductSpecification.hasMinimumPrice(
                            filterRequest.minPrice()
                    )
            );
        }

        if (filterRequest.maxPrice() != null) {

            specification = specification.and(
                    ProductSpecification.hasMaximumPrice(
                            filterRequest.maxPrice()
                    )
            );
        }

        if (filterRequest.name() != null && !filterRequest.name().isBlank()) {
            specification = specification.and(
                    ProductSpecification.nameContains(filterRequest.name())
            );
        }

        return specification;

    }
}
