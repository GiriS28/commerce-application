package com.nisum.cartservice.exception;

public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(Long productId) {
        super("Product " + productId + " is not available");
    }
}