package com.nisum.productservice.event;

public record ProductUpdatedTransactionEvent(
        Long productId
) {
}
