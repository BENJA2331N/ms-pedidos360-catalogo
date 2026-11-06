package com.pedidos360.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockUpdateDTO {

    private Long productId;
    private String productoNombre;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad a descontar debe ser mayor a 0")
    private Integer cantidad;

    private Integer stockRestante;
    private String motivo;

    public StockUpdateDTO() {
    }

    public StockUpdateDTO(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public StockUpdateDTO(Long productId, String productoNombre, Integer cantidad, Integer stockRestante) {
        this.productId = productId;
        this.productoNombre = productoNombre;
        this.cantidad = cantidad;
        this.stockRestante = stockRestante;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getStockRestante() {
        return stockRestante;
    }

    public void setStockRestante(Integer stockRestante) {
        this.stockRestante = stockRestante;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    @Override
    public String toString() {
        return "StockUpdateDTO{productId=" + productId
                + ", productoNombre='" + productoNombre + '\''
                + ", cantidad=" + cantidad
                + ", stockRestante=" + stockRestante
                + (motivo != null ? ", motivo='" + motivo + '\'' : "")
                + '}';
    }
}
