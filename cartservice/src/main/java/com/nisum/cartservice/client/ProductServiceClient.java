package com.nisum.cartservice.client;

import com.nisum.cartservice.config.ResilienceConfig;
import com.nisum.cartservice.dto.response.ProductResponse;
import com.nisum.cartservice.exception.ProductNotFoundException;
import com.nisum.cartservice.exception.ProductServiceException;
/*import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;*/
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;


import java.util.function.Supplier;

@Component
public class ProductServiceClient {

    private final RestClient restClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private static final Logger log =
            LoggerFactory.getLogger(ProductServiceClient.class);

    public ProductServiceClient(RestClient restClient, Retry retry, CircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
    }

    public ProductResponse getProduct(Long productId) {

        Supplier<ProductResponse> supplier =
                () -> getProductFromService(productId);

        Supplier<ProductResponse> retrySupplier =
                Retry.decorateSupplier(
                        retry,
                        supplier
                );
        Supplier<ProductResponse> productSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                retrySupplier
        );
        return productSupplier.get();
    }

    private ProductResponse getProductFromService(Long productId) {

        log.info("Calling Product Service for productId={}", productId);


        try {

            return restClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {

            throw new ProductNotFoundException(productId);

        } catch (HttpServerErrorException exception) {

            throw new ProductServiceException(
                    "Product Service returned a server error",
                    exception
            );
        } catch (ResourceAccessException exception) {

            throw new ProductServiceException(
                    "Product Service is currently unavailable",
                    exception
            );
        }
    }
}
/*@Component
@Slf4j
public class ProductServiceClient {

    private final RestClient restClient;

    public ProductServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService")
    public ProductResponse getProduct(Long productId) {

        return getProductFromService(productId);
    }

    private ProductResponse getProductFromService(Long productId) {

        log.info(
                "Calling Product Service for productId={}",
                productId
        );

        try {

            return restClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {

            throw new ProductNotFoundException(productId);

        } catch (HttpServerErrorException exception) {

            throw new ProductServiceException(
                    "Product Service returned a server error",
                    exception
            );

        } catch (ResourceAccessException exception) {

            throw new ProductServiceException(
                    "Product Service is currently unavailable",
                    exception
            );
        }
    }*/
//}