package com.pedidos360.catalog.repository;

import com.pedidos360.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActivoTrue();
    Optional<Product> findByIdAndActivoTrue(Long id);
}
