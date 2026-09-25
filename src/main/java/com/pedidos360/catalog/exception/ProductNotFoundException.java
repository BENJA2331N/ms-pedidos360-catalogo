package com.pedidos360.catalog.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Producto con ID " + id + " no encontrado o inactivo.");
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
