package com.nisum.productservice.consumer;

import com.nisum.productservice.cache.ProductCacheEntry;
import com.nisum.productservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private static final String PRODUCT_EVENTS_TOPIC =
            "product-events-keyed";

    private static final String PRODUCT_EVENTS_DLT =
            "product-events-keyed.DLT";

    private static final String PRODUCT_CACHE_PREFIX =
            "product:";

    private final RedisTemplate<String, ProductCacheEntry>
            productRedisTemplate;

    @KafkaListener(
            topics = PRODUCT_EVENTS_TOPIC,
            groupId = "product-cache-group"
    )
    public void consume(ProductUpdatedEvent event) {

        Long productId = event.productId();

        String cacheKey =
                PRODUCT_CACHE_PREFIX + productId;

        log.info(
                "ProductUpdated event received: productId={}, cacheKey={}",
                productId,
                cacheKey
        );

        Boolean evicted =
                productRedisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(evicted)) {

            log.info(
                    "Product cache evicted by Kafka consumer: productId={}, key={}",
                    productId,
                    cacheKey
            );

        } else {

            log.info(
                    "Product cache key was not present: productId={}, key={}",
                    productId,
                    cacheKey
            );
        }
    }

    @KafkaListener(
            topics = PRODUCT_EVENTS_DLT,
            groupId = "product-cache-dlt-group",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void consumeDlt(ProductUpdatedEvent event) {

        Long productId = event.productId();

        String cacheKey =
                PRODUCT_CACHE_PREFIX + productId;

        log.info(
                "DLT event received: productId={}, cacheKey={}",
                productId,
                cacheKey
        );

        Boolean evicted =
                productRedisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(evicted)) {

            log.info(
                    "Product cache evicted from DLT: productId={}, key={}",
                    productId,
                    cacheKey
            );

        } else {

            log.info(
                    "Product cache key was not present in DLT recovery: productId={}, key={}",
                    productId,
                    cacheKey
            );
        }
    }
}