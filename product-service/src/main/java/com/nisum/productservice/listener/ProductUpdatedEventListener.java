package com.nisum.productservice.listener;

import com.nisum.productservice.event.ProductUpdatedTransactionEvent;
import com.nisum.productservice.producer.ProductEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductUpdatedEventListener {

    private final ProductEventProducer productEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdated(
            ProductUpdatedTransactionEvent event) {

        log.info(
                "Product transaction committed, publishing Kafka event: productId={}",
                event.productId()
        );

        productEventProducer.publishProductUpdated(
                event.productId()
        );
    }
}