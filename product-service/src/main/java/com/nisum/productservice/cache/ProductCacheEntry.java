package com.nisum.productservice.cache;

import com.nisum.productservice.dto.ProductResponse;

public record ProductCacheEntry(
        boolean found,
        ProductResponse product
) {

    public static ProductCacheEntry found(ProductResponse product) {
        return new ProductCacheEntry(true, product);
    }

    public static ProductCacheEntry notFound() {
        return new ProductCacheEntry(false, null);
    }
}