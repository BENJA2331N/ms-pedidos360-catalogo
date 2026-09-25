package com.pedidos360.catalog.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int stockActual, int cantidadRequerida) {
        super(String.format("Stock insuficiente para el producto ID %d. Stock actual: %d, cantidad requerida: %d",
                productId, stockActual, cantidadRequerida));
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}
