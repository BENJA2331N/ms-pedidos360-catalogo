package com.pedidos360.catalog.messaging.inventario;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consumidor dedicado a la Dead Letter Queue (DLQ) del dominio "Inventario / Stock".
 * Registra en logs los mensajes que no pudieron ser procesados y su causa ({@code x-death}),
 * confirmándolos explícitamente para evitar reprocesamiento infinito.
 */
@Component
public class CatalogStockDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogStockDlqConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.catalogo.dlq}")
    public void registrarDlqCatalogo(Message mensaje, Channel channel) throws IOException {
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Object motivo = mensaje.getMessageProperties().getHeader("x-death");
        long tag = mensaje.getMessageProperties().getDeliveryTag();
        log.error("[DLQ INVENTARIO] Mensaje no entregado registrado en DLQ. cuerpo={} x-death={}", cuerpo, motivo);
        channel.basicAck(tag, false);
    }
}
