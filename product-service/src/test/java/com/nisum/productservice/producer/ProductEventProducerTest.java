package com.nisum.productservice.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductEventProducerTest {

    @Autowired
    private ProductEventProducer productEventProducer;

    @Test
    void shouldPublishProductUpdatedEvent() {

        productEventProducer.publishProductUpdated(3L);
    }
}