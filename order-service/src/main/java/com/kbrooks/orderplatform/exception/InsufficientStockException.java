package com.kbrooks.orderplatform.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, String detail) {
        super("Insufficient stock for product '" + productId + "': " + detail);
    }
}
