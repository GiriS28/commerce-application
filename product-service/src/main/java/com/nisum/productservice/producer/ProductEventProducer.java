package com.nisum.productservice.producer;

import com.nisum.productservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private final KafkaTemplate<Long, ProductUpdatedEvent> kafkaTemplate;

    private static final String PRODUCT_EVENTS_TOPIC = "product-events-keyed";

    public void publishProductUpdated(Long productId) {

        ProductUpdatedEvent event =
                new ProductUpdatedEvent(productId);

        kafkaTemplate.send(
                PRODUCT_EVENTS_TOPIC,
                productId,
                event
        );

        log.info(
                "ProductUpdated event published: productId={}, topic={}",
                productId,
                PRODUCT_EVENTS_TOPIC
        );
    }
}
