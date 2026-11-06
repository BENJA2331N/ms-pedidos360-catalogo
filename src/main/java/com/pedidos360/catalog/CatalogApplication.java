package com.pedidos360.catalog;

import com.pedidos360.catalog.model.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }

    @Bean
    public CommandLineRunner initCatalogData(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                    new Product(null, "Notebook Gamer ASUS ROG Strix G16", "Intel Core i7-13650HX, 16GB DDR5, 1TB SSD NVMe, RTX 4060 8GB", new BigDecimal("1299990.00"), 12, true),
                    new Product(null, "Monitor LG UltraGear 27\" QHD 165Hz", "Panel IPS 1ms, G-Sync Compatible, HDR10, DisplayPort + HDMI", new BigDecimal("269990.00"), 25, true),
                    new Product(null, "Teclado MecÃ¡nico Keychron K8 Pro RGB", "Switches Gateron Red Hot-Swappable, Bluetooth 5.1 / USB-C, Aluminio", new BigDecimal("94990.00"), 35, true),
                    new Product(null, "Mouse InalÃ¡mbrico Logitech G Pro X Superlight", "Sensor HERO 25K, ultraligero 63g, baterÃ­a 70 horas", new BigDecimal("114990.00"), 40, true),
                    new Product(null, "AudÃ­fonos Sony WH-1000XM5 Noise Cancelling", "CancelaciÃ³n activa de ruido lÃ­der, 30h baterÃ­a, carga rÃ¡pida USB-C", new BigDecimal("299990.00"), 18, true),
                    new Product(null, "Silla ErgonÃ³mica Cougar Argo One", "Soporte lumbar dinÃ¡mico, malla transpirable, reposabrazos 3D", new BigDecimal("349990.00"), 8, true)
                ));
            }
        };
    }
}