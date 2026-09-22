package com.nisum.cartservice.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartNotFound(
            CartNotFoundException exception,
            HttpServletRequest request) {

        log.warn(
                "Cart not found: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse error = new ErrorResponse(
                "CART_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartItemNotFound(
            CartItemNotFoundException exception,
            HttpServletRequest request) {

        log.warn(
                "Cart item not found: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse error = new ErrorResponse(
                "CART_ITEM_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        log.warn(
                "Request validation failed: path={}, message={}",
                request.getRequestURI(),
                message
        );

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleProductNotAvailable(
            ProductNotAvailableException exception,
            HttpServletRequest request) {

        log.warn(
                "Product not available: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse error = new ErrorResponse(
                "PRODUCT_NOT_AVAILABLE",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(CartStorageException.class)
    public ResponseEntity<ErrorResponse> handleCartStorageException(
            CartStorageException exception,
            HttpServletRequest request) {

        log.error(
                "Cart storage failure: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );

        ErrorResponse error = new ErrorResponse(
                "CART_STORAGE_UNAVAILABLE",
                "Cart service is temporarily unavailable",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            ProductNotFoundException exception,
            HttpServletRequest request) {

        log.warn(
                "Product not found: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse error = new ErrorResponse(
                "PRODUCT_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<ErrorResponse> handleProductServiceException(
            ProductServiceException exception,
            HttpServletRequest request) {

        log.error(
                "Product service failure: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );

        ErrorResponse error = new ErrorResponse(
                "PRODUCT_SERVICE_ERROR",
                "Product service is currently experiencing an internal error",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermittedException(
            CallNotPermittedException exception,
            HttpServletRequest request) {

        log.warn(
                "Product service circuit breaker is OPEN: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse errorResponse = new ErrorResponse(
                "PRODUCT_SERVICE_UNAVAILABLE",
                "Product service is temporarily unavailable. Please try again later.",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request) {

        log.error(
                "Unexpected error: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}