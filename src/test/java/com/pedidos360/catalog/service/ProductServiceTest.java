package com.pedidos360.catalog.service;

import com.pedidos360.catalog.dto.ProductRequestDTO;
import com.pedidos360.catalog.dto.ProductResponseDTO;
import com.pedidos360.catalog.dto.StockUpdateDTO;
import com.pedidos360.catalog.exception.InsufficientStockException;
import com.pedidos360.catalog.exception.ProductNotFoundException;
import com.pedidos360.catalog.messaging.producer.CatalogStockProducer;
import com.pedidos360.catalog.model.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogStockProducer catalogStockProducer;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Debe crear un producto activo y devolver su DTO")
    void createProductSuccess() {
        ProductRequestDTO req = new ProductRequestDTO("Monitor 27\"", "IPS 144Hz", new BigDecimal("249990"), 15);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponseDTO res = productService.create(req);

        assertEquals(1L, res.getId());
        assertEquals("Monitor 27\"", res.getNombre());
        assertEquals(15, res.getStock());
        assertTrue(res.getActivo());
    }

    @Test
    @DisplayName("Debe listar únicamente productos activos")
    void findAllActiveProducts() {
        Product p = new Product(2L, "Mouse Pro", "Inalámbrico", new BigDecimal("49990"), 30, true);
        when(productRepository.findByActivoTrue()).thenReturn(List.of(p));

        List<ProductResponseDTO> list = productService.findAll();

        assertEquals(1, list.size());
        assertEquals("Mouse Pro", list.get(0).getNombre());
    }

    @Test
    @DisplayName("Descontar stock persiste el nuevo saldo y publica evento asíncrono en RabbitMQ")
    void decreaseStockPublishesRabbitEvent() {
        Product p = new Product(5L, "Teclado Mecánico", "Switch Red", new BigDecimal("89990"), 10, true);
        when(productRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        productService.decreaseStock(5L, 3);

        assertEquals(7, p.getStock());
        verify(productRepository).save(p);

        ArgumentCaptor<StockUpdateDTO> captor = ArgumentCaptor.forClass(StockUpdateDTO.class);
        verify(catalogStockProducer).publicarEventoStock(captor.capture());
        StockUpdateDTO evento = captor.getValue();
        assertEquals(5L, evento.getProductId());
        assertEquals(3, evento.getCantidad());
        assertEquals(7, evento.getStockRestante());
    }

    @Test
    @DisplayName("Descontar stock mayor al disponible lanza InsufficientStockException sin publicar evento")
    void decreaseStockInsufficientThrowsException() {
        Product p = new Product(5L, "Teclado Mecánico", "Switch Red", new BigDecimal("89990"), 2, true);
        when(productRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(p));

        assertThrows(InsufficientStockException.class, () -> productService.decreaseStock(5L, 5));

        verify(productRepository, never()).save(any());
        verifyNoInteractions(catalogStockProducer);
    }

    @Test
    @DisplayName("Buscar producto inexistente lanza ProductNotFoundException")
    void findByIdNotFoundThrowsException() {
        when(productRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(99L));
    }
}
