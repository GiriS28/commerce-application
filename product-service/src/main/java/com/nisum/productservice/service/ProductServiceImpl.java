package com.nisum.productservice.service;

import com.nisum.productservice.cache.ProductCacheEntry;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    private final RedisTemplate<String, ProductCacheEntry> productRedisTemplate;

    @Value("${product.cache.ttl}")
    private Duration productCacheTtl;

    @Value("${product.cache.negative-ttl}")
    private Duration productNegativeCacheTtl;

    private final ConcurrentHashMap<Long, Object> productLocks = new ConcurrentHashMap<>();

    private static final String PRODUCT_CACHE_PREFIX = "product:";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductSortValidator productSortValidator;
    private final ProductFilterValidator productFilterValidator;
    private final ProductSpecificationBuilder productSpecificationBuilder;

    private static final Logger log =
            LoggerFactory.getLogger(ProductServiceImpl.class);

    public ProductServiceImpl(RedisTemplate<String, ProductCacheEntry> productRedisTemplate, ProductRepository productRepository,
                              ProductMapper productMapper, ProductSortValidator productSortValidator, ProductFilterValidator productFilterValidator, ProductSpecificationBuilder productSpecificationBuilder) {
        this.productRedisTemplate = productRedisTemplate;
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

        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        log.info("Checking product cache: key={}", cacheKey);

        /*try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/

        // first cache check
        try {

            ProductCacheEntry cachedEntry =
                    productRedisTemplate.opsForValue().get(cacheKey);

            if (cachedEntry != null) {

                if (!cachedEntry.found()) {

                    log.info(
                            "Product negative cache HIT: productId={}, key={}",
                            id,
                            cacheKey
                    );

                    throw new ProductNotFoundException(id);
                }

                log.info(
                        "Product cache HIT: productId={}, key={}",
                        id,
                        cacheKey
                );

                return cachedEntry.product();
            }

            log.info(
                    "Product cache MISS: productId={}, key={}",
                    id,
                    cacheKey
            );

        } catch (ProductNotFoundException ex) {

            throw ex;

        } catch (Exception ex) {

            log.warn(
                    "Redis unavailable. Falling back to database: productId={}, error={}",
                    id,
                    ex.getMessage()
            );
        }
        Object lock = productLocks.computeIfAbsent(
                id,
                key -> new Object()
        );

        synchronized (lock) {

            log.info(
                    "Acquired product load lock: productId={}",
                    id
            );

            // SECOND CACHE CHECK
            try {

                ProductCacheEntry cachedEntry =
                        productRedisTemplate.opsForValue().get(cacheKey);

                if (cachedEntry != null) {

                    if (!cachedEntry.found()) {

                        log.info(
                                "Product negative cache HIT after lock: productId={}",
                                id
                        );

                        throw new ProductNotFoundException(id);
                    }

                    log.info(
                            "Product cache HIT after lock: productId={}",
                            id
                    );

                    return cachedEntry.product();
                }

            } catch (ProductNotFoundException ex) {

                throw ex;

            } catch (Exception ex) {

                log.warn(
                        "Redis unavailable during second cache check: productId={}, error={}",
                        id,
                        ex.getMessage()
                );
            }

            // Only one request should reach here
            log.info(
                    "Loading product from database after cache miss: productId={}",
                    id
            );

            log.info("Fetching product from database: id={}", id);

            Product product = productRepository.findById(id)
                    .orElse(null);

            if (product == null) {

                log.warn("Product not found: id={}", id);

                try {

                    productRedisTemplate.opsForValue().set(
                            cacheKey,
                            ProductCacheEntry.notFound(),
                            productNegativeCacheTtl
                    );

                    log.info(
                            "Product negative result cached: productId={}, ttl={}",
                            id,
                            productNegativeCacheTtl
                    );

                } catch (Exception ex) {

                    log.warn(
                            "Unable to cache negative product result: productId={}, error={}",
                            id,
                            ex.getMessage()
                    );
                }

                throw new ProductNotFoundException(id);
            }

            ProductResponse response =
                    productMapper.toResponse(product);

            try {

                productRedisTemplate.opsForValue().set(
                        cacheKey,
                        ProductCacheEntry.found(response),
                        productCacheTtl
                );

                log.info(
                        "Product cached successfully: productId={}, ttl={}",
                        id,
                        productCacheTtl
                );

            } catch (Exception ex) {

                log.warn(
                        "Unable to cache product. Returning database response: productId={}, error={}",
                        id,
                        ex.getMessage()
                );
            }

            return response;
        }
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

        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        evictProductCacheWithRetry(id, cacheKey);

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deactivateProduct(Long id) {

        log.info("Deactivating product: id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStatus(ProductStatus.INACTIVE);

        productRepository.save(product);

        log.info("Product deactivated successfully: id={}", id);

        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        evictProductCacheWithRetry(id, cacheKey);
    }

    private static void isCacheEvicted(Long id, Boolean evicted, String cacheKey) {
        if (Boolean.TRUE.equals(evicted)) {

            log.info(
                    "Product cache evicted: productId={}, key={}",
                    id,
                    cacheKey
            );

        } else {

            log.info(
                    "Product cache eviction skipped: key={} was not present",
                    cacheKey
            );
        }
    }

    private void evictProductCacheWithRetry(Long id, String cacheKey) {

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                Boolean evicted =
                        productRedisTemplate.delete(cacheKey);

                if (Boolean.TRUE.equals(evicted)) {

                    log.info(
                            "Product cache evicted successfully: productId={}, key={}, attempt={}",
                            id,
                            cacheKey,
                            attempt
                    );

                    return;
                }

                log.info(
                        "Product cache eviction skipped: key={} was not present, attempt={}",
                        cacheKey,
                        attempt
                );

                return;

            } catch (Exception exception) {

                log.warn(
                        "Product cache eviction failed: productId={}, key={}, attempt={}/{}, error={}",
                        id,
                        cacheKey,
                        attempt,
                        maxAttempts,
                        exception.getMessage()
                );

                if (attempt == maxAttempts) {

                    log.error(
                            "Product cache eviction permanently failed after {} attempts: productId={}, key={}",
                            maxAttempts,
                            id,
                            cacheKey,
                            exception
                    );

                    return;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    log.error(
                            "Product cache eviction retry interrupted: productId={}, key={}",
                            id,
                            cacheKey,
                            interruptedException
                    );

                    return;
                }
            }
        }
    }
}