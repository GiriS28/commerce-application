package com.nisum.productservice.producer;

import com.nisum.productservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private static final String PRODUCT_EVENTS_TOPIC =
            "product-events-keyed";

    private final KafkaTemplate<Long, ProductUpdatedEvent> kafkaTemplate;

    public void publishProductUpdated(Long productId) {

        ProductUpdatedEvent event =
                new ProductUpdatedEvent(productId);

        try {

            kafkaTemplate
                    .send(
                            PRODUCT_EVENTS_TOPIC,
                            productId,
                            event
                    )
                    .get(10, TimeUnit.SECONDS);

            log.info(
                    "ProductUpdated event published successfully: productId={}, topic={}",
                    productId,
                    PRODUCT_EVENTS_TOPIC
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to publish ProductUpdated event: productId={}, topic={}",
                    productId,
                    PRODUCT_EVENTS_TOPIC,
                    ex
            );

            throw new RuntimeException(
                    "Failed to publish product event to Kafka",
                    ex
            );
        }
    }
}