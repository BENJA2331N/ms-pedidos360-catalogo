package com.pedidos360.catalog.service;

import com.pedidos360.catalog.dto.ProductRequestDTO;
import com.pedidos360.catalog.dto.ProductResponseDTO;
import com.pedidos360.catalog.exception.InsufficientStockException;
import com.pedidos360.catalog.exception.ProductNotFoundException;
import com.pedidos360.catalog.model.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAll() {
        return productRepository.findByActivoTrue()
                .stream()
                .map(ProductResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO findById(Long id) {
        Product product = productRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponseDTO.fromEntity(product);
    }

    public ProductResponseDTO create(ProductRequestDTO dto) {
        Product product = new Product();
        product.setNombre(dto.getNombre());
        product.setDescripcion(dto.getDescripcion());
        product.setPrecio(dto.getPrecio());
        product.setStock(dto.getStock());
        product.setActivo(true);

        Product saved = productRepository.save(product);
        return ProductResponseDTO.fromEntity(saved);
    }

    public ProductResponseDTO update(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setNombre(dto.getNombre());
        product.setDescripcion(dto.getDescripcion());
        product.setPrecio(dto.getPrecio());
        product.setStock(dto.getStock());

        Product updated = productRepository.save(product);
        return ProductResponseDTO.fromEntity(updated);
    }

    public void delete(Long id) {
        Product product = productRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Soft delete: no borramos el registro físicamente
        product.setActivo(false);
        productRepository.save(product);
    }

    public void decreaseStock(Long id, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
        }

        Product product = productRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (product.getStock() - cantidad < 0) {
            throw new InsufficientStockException(id, product.getStock(), cantidad);
        }

        product.setStock(product.getStock() - cantidad);
        productRepository.save(product);
    }
}
