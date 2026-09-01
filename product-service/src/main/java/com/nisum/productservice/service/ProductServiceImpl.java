package com.nisum.productservice.service;

import com.nisum.productservice.dto.CreateProductRequest;
import com.nisum.productservice.dto.ProductFilterRequest;
import com.nisum.productservice.dto.ProductResponse;
import com.nisum.productservice.dto.UpdateProductRequest;
import com.nisum.productservice.entity.Product;
import com.nisum.productservice.entity.ProductStatus;
import com.nisum.productservice.exception.DuplicateSkuException;
import com.nisum.productservice.exception.ProductNotFoundException;
import com.nisum.productservice.mapper.ProductMapper;
import com.nisum.productservice.repository.ProductRepository;
import com.nisum.productservice.specification.ProductSpecificationBuilder;
import com.nisum.productservice.validation.ProductFilterValidator;
import com.nisum.productservice.validation.ProductSortValidator;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductSortValidator productSortValidator;
    private final ProductFilterValidator productFilterValidator;
    private final ProductSpecificationBuilder productSpecificationBuilder;

    private static final Logger log =
            LoggerFactory.getLogger(ProductServiceImpl.class);

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductMapper productMapper, ProductSortValidator productSortValidator, ProductFilterValidator productFilterValidator, ProductSpecificationBuilder productSpecificationBuilder) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.productSortValidator = productSortValidator;
        this.productFilterValidator = productFilterValidator;
        this.productSpecificationBuilder = productSpecificationBuilder;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = productMapper.toEntity(request);

        Product savedProduct = productRepository.save(product);

        log.info(
                "Product created successfully: id={}, sku={}",
                savedProduct.getId(),
                savedProduct.getSku()
        );

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {

        log.info("Fetching product with id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() ->{
                    log.warn("Product not found: id={}", id);
                    return new ProductNotFoundException(id);
                });

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            ProductFilterRequest filterRequest, Pageable pageable) {

        if (isFilterApplied(filterRequest)) {

            log.info(
                    "Searching products with filters: name={}, category={}, status={}, minPrice={}, maxPrice={}, page={}, size={}",
                    filterRequest.name(),
                    filterRequest.category(),
                    filterRequest.status(),
                    filterRequest.minPrice(),
                    filterRequest.maxPrice(),
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

        } else {

            log.info(
                    "Fetching all products: page={}, size={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }

        productFilterValidator.validate(filterRequest);
        productSortValidator.validate(pageable);

        Specification<Product> buildSpecification = productSpecificationBuilder.build(filterRequest);

        return productRepository
                .findAll(buildSpecification, pageable)
                .map(productMapper::toResponse);
    }

    private static boolean isFilterApplied(ProductFilterRequest filterRequest) {
        return filterRequest.name() != null ||
                filterRequest.category() != null ||
                filterRequest.status() != null ||
                filterRequest.minPrice() != null ||
                filterRequest.maxPrice() != null;
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request) {

        log.info(
                "Updating product: id={}, requestVersion={}",
                id,
                request.version()
        );

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getVersion().equals(request.version())) {

            log.warn(
                    "Optimistic locking conflict: productId={}, requestedVersion={}, currentVersion={}",
                    id,
                    request.version(),
                    product.getVersion()
            );
            throw new OptimisticLockException(
                    "Product was modified by another request"
            );
        }

        productMapper.updateEntity(product, request);

        Product updatedProduct = productRepository.saveAndFlush(product);

        log.info(
                "Product updated successfully: id={}, newVersion={}",
                updatedProduct.getId(),
                updatedProduct.getVersion()
        );

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deactivateProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStatus(ProductStatus.INACTIVE);

        productRepository.save(product);
    }
}