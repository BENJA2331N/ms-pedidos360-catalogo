package com.pedidos360.catalog.controller;

import com.pedidos360.catalog.dto.ProductRequestDTO;
import com.pedidos360.catalog.dto.ProductResponseDTO;
import com.pedidos360.catalog.dto.StockUpdateDTO;
import com.pedidos360.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Listar todos los productos activos.
     * Roles esperados: Cliente, Operador, Admin
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<ProductResponseDTO> products = productService.findAll();
        return ResponseEntity.ok(products);
    }

    /**
     * Obtener detalle de un producto por ID.
     * Roles esperados: Cliente, Operador, Admin
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.findById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Crear un nuevo producto.
     * Roles esperados: Admin
     */
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO createdProduct = productService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    /**
     * Editar un producto existente.
     * Roles esperados: Admin, Operador
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO updatedProduct = productService.update(id, dto);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Eliminación lógica (soft-delete) de un producto.
     * Roles esperados: Admin
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Descontar stock de un producto (endpoint interno llamado por ms-pedidos / orders).
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> decreaseStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateDTO dto) {
        productService.decreaseStock(id, dto.getCantidad());
        return ResponseEntity.ok(Map.of(
                "message", "Stock actualizado exitosamente",
                "productId", id,
                "descontado", dto.getCantidad()
        ));
    }
}
