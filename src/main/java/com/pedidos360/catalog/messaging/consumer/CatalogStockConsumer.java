package com.pedidos360.catalog.messaging.consumer;

import com.pedidos360.catalog.dto.StockUpdateDTO;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CatalogStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogStockConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.catalogo.queue}")
    public void consumirEventoStock(StockUpdateDTO dto,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            log.info("Recibido evento de stock en consumidor de catálogo: {}", dto);
            if (dto == null) {
                throw new IllegalArgumentException("Datos de stock nulos");
            }
            channel.basicAck(tag, false);
            log.info("Evento de stock confirmado (ACK) exitosamente.");
        } catch (Exception ex) {
            log.error("Error al procesar evento de stock. Enviando a DLQ: {}", ex.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.catalogo.dlq}")
    public void registrarDlqCatalogo(Object mensajeFallido,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.warn("[DLQ CATALOGO LOG] Mensaje no entregado registrado en DLQ: {}", mensajeFallido);
        channel.basicAck(tag, false);
    }
}
