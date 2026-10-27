package com.pedidos360.catalog.messaging.producer;

import com.pedidos360.catalog.dto.StockUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de stock. Los nombres de exchange y routing key vienen de application.yml.
 * <p>
 * La mensajería es una tarea secundaria y asíncrona: si RabbitMQ no está disponible, el fallo se
 * registra en logs pero NO se propaga, para no afectar (ni revertir) el descuento de stock.
 */
@Component
public class CatalogStockProducer {

    private static final Logger log = LoggerFactory.getLogger(CatalogStockProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.catalogo.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.catalogo.routing-key}")
    private String routingKey;

    public CatalogStockProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarEventoStock(StockUpdateDTO dto) {
        log.info("Publicando evento de actualización de stock en RabbitMQ: {}", dto);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, dto);
        } catch (AmqpException ex) {
            log.error("No se pudo publicar el evento de stock en RabbitMQ (el descuento de stock no se ve afectado): {}",
                    ex.getMessage());
        }
    }
}
