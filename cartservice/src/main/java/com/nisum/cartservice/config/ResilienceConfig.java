package com.nisum.cartservice.config;

import com.nisum.cartservice.exception.ProductServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Logger log =
            LoggerFactory.getLogger(ResilienceConfig.class);

    @Value("${resilience4j.retry.instances.productService.max-attempts}")
    private int maxAttempts;

    @Value("${resilience4j.retry.instances.productService.wait-duration}")
    private Duration waitDuration;

    @Value("${resilience4j.retry.instances.productService.exponential-backoff-multiplier}")
    private double exponentialBackoffMultiplier;

    @Bean
    public Retry productServiceRetry() {

        IntervalFunction intervalFunction =
                IntervalFunction.ofExponentialBackoff(
                        waitDuration.toMillis(),
                        exponentialBackoffMultiplier
                );

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFunction)
                .retryExceptions(ProductServiceException.class)
                .build();

        Retry retry = Retry.of("productService", config);

        retry.getEventPublisher()
                .onRetry(event ->
                        log.warn(
                                "Product Service retry: attempt={}, waitInterval={}ms, cause={}",
                                event.getNumberOfRetryAttempts(),
                                event.getWaitInterval().toMillis(),
                                event.getLastThrowable()
                                        .getClass()
                                        .getSimpleName()
                        )
                );

        return retry;
    }

    @Bean
    public CircuitBreaker productServiceCircuitBreaker() {

        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .slidingWindowType(
                                CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                        )
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build();

        CircuitBreaker circuitBreaker =
                CircuitBreaker.of("productService", config);
        log.info(
                "Product Service Circuit Breaker initial state: {}",
                circuitBreaker.getState()
        );

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.warn(
                                "CB STATE CHANGE: {}",
                                event.getStateTransition()
                        )
                )
                .onError(event ->
                        log.warn(
                                "CB ERROR: duration={}ms, exception={}",
                                event.getElapsedDuration().toMillis(),
                                event.getThrowable().getClass().getSimpleName()
                        )
                )
                .onSuccess(event ->
                        log.info(
                                "CB SUCCESS: duration={}ms",
                                event.getElapsedDuration().toMillis()
                        )
                )
                .onCallNotPermitted(event ->
                        log.warn(
                                "CB CALL NOT PERMITTED"
                        )
                );

        return circuitBreaker;
    }

   /* // Annotation based
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceConfig(
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {

        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;

        configureRetryLogging();
        configureCircuitBreakerLogging();
    }

    private void configureRetryLogging() {

        Retry retry = retryRegistry.retry("productService");

        retry.getEventPublisher()
                .onRetry(event ->
                        log.warn(
                                "Product Service retry: attempt={}, waitInterval={}ms, cause={}",
                                event.getNumberOfRetryAttempts(),
                                event.getWaitInterval().toMillis(),
                                event.getLastThrowable()
                                        .getClass()
                                        .getSimpleName()
                        )
                );
    }

    private void configureCircuitBreakerLogging() {

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("productService");

        log.info(
                "Product Service Circuit Breaker initial state: {}",
                circuitBreaker.getState()
        );

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.warn(
                                "CB STATE CHANGE: {}",
                                event.getStateTransition()
                        )
                )
                .onError(event ->
                        log.warn(
                                "CB ERROR: duration={}ms, exception={}",
                                event.getElapsedDuration().toMillis(),
                                event.getThrowable()
                                        .getClass()
                                        .getSimpleName()
                        )
                )
                .onSuccess(event ->
                        log.info(
                                "CB SUCCESS: duration={}ms",
                                event.getElapsedDuration().toMillis()
                        )
                )
                .onCallNotPermitted(event ->
                        log.warn("CB CALL NOT PERMITTED")
                );
    }*/
}
