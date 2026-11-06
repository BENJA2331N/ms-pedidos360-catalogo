package com.pedidos360.catalog.config;

import com.pedidos360.catalog.model.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initCatalogProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.findByActivoTrue().isEmpty()) {
                log.info("El catálogo no tiene productos activos. Insertando productos iniciales...");
                List<Product> initialProducts = List.of(
                        new Product(null, "Laptop Dell Precision 5570", "Workstation empresarial Core i7, 32GB RAM, 1TB NVMe", new BigDecimal("1450000"), 15, true),
                        new Product(null, "Monitor UltraSharp 27\" 4K USB-C", "Monitor IPS UHD con Hub USB-C 90W Power Delivery", new BigDecimal("420000"), 25, true),
                        new Product(null, "Teclado Mecánico Logitech MX Mechanical", "Teclado inalámbrico retroiluminado táctil silencioso", new BigDecimal("135000"), 40, true),
                        new Product(null, "Mouse Logitech MX Master 3S", "Mouse ergonómico inalámbrico 8000 DPI silencioso", new BigDecimal("95000"), 50, true),
                        new Product(null, "Docking Station Thunderbolt 4 Dell WD22TB4", "Estación de acoplamiento modular Thunderbolt 4 130W", new BigDecimal("260000"), 20, true),
                        new Product(null, "Auriculares Jabra Evolve2 65", "Headset inalámbrico con cancelación de ruido certificada MS Teams", new BigDecimal("180000"), 30, true)
                );
                productRepository.saveAll(initialProducts);
                log.info("Se cargaron {} productos iniciales en el catálogo.", initialProducts.size());
            } else {
                log.info("El catálogo ya cuenta con productos activos registrados.");
            }
        };
    }
}

