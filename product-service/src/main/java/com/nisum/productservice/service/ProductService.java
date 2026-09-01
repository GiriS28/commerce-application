package com.nisum.productservice.service;

import com.nisum.productservice.dto.CreateProductRequest;
import com.nisum.productservice.dto.ProductFilterRequest;
import com.nisum.productservice.dto.ProductResponse;
import com.nisum.productservice.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProduct(Long id);

    Page<ProductResponse> getAllProducts(ProductFilterRequest filterRequest, Pageable pageable);

    ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request);

    void deactivateProduct(Long id);
}