package com.pedidos360.catalog.messaging.producer;

import com.pedidos360.catalog.dto.StockUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        rabbitTemplate.convertAndSend(exchange, routingKey, dto);
    }
}
