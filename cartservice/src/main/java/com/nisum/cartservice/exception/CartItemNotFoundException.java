package com.nisum.cartservice.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long productId) {
        super("Product " + productId + " is not present in cart");
    }
}