package com.nisum.cartservice.exception;

public class CartStorageException extends RuntimeException {

    public CartStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}