package com.nisum.productservice.service;

import com.nisum.productservice.dto.CreateProductRequest;
import com.nisum.productservice.dto.ProductResponse;
import com.nisum.productservice.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProduct(Long id);

    List<ProductResponse> getAllProducts();

    ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request);

    void deactivateProduct(Long id);
}