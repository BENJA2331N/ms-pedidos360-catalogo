package com.pedidos360.catalog.messaging.consumer;

import com.pedidos360.catalog.dto.StockUpdateDTO;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consumidores del dominio "catálogo / stock" (ACK manual configurado en application.yml).
 * <p>
 * Política de confirmación y errores de la cola principal:
 * <ul>
 *   <li><b>Éxito</b> -> {@code basicAck}.</li>
 *   <li><b>Error no recuperable</b> (mensaje inválido, {@link IllegalArgumentException}) ->
 *       {@code basicNack(requeue=false)}: RabbitMQ lo desvía a la DLQ vía x-dead-letter-exchange.</li>
 *   <li><b>Error inesperado</b> (posiblemente transitorio) -> un único <b>reintento</b>
 *       ({@code basicNack(requeue=true)}); si el mensaje ya venía reentregado -> {@code basicNack(requeue=false)} a la DLQ.</li>
 * </ul>
 * Los mensajes que llegan a la DLQ se registran en logs y se confirman.
 */
@Component
public class CatalogStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogStockConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.catalogo.queue}")
    public void consumirEventoStock(StockUpdateDTO dto,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                    @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered)
            throws IOException {
        try {
            log.info("Recibido evento de stock en consumidor de catálogo: {}", dto);
            if (dto == null) {
                throw new IllegalArgumentException("Datos de stock nulos");
            }
            if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cantidad de stock inválida: " + dto.getCantidad());
            }
            // Confirmación explícita (ACK)
            channel.basicAck(tag, false);
            log.info("Evento de stock confirmado (ACK) exitosamente.");
        } catch (IllegalArgumentException ex) {
            log.error("Evento de stock inválido. Rechazando (NACK) y enviando a DLQ: {}", ex.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            if (!Boolean.TRUE.equals(redelivered)) {
                log.warn("Fallo inesperado al procesar evento de stock. Reintentando una vez (NACK con requeue): {}",
                        ex.getMessage());
                channel.basicNack(tag, false, true);
            } else {
                log.error("Fallo persistente tras reintento. Enviando a DLQ (NACK sin requeue): {}", ex.getMessage());
                channel.basicNack(tag, false, false);
            }
        }
    }

    /**
     * Registra en logs los mensajes no entregados. Recibe el mensaje crudo (sin convertir) para poder
     * registrar también mensajes con JSON inválido.
     */
    @RabbitListener(queues = "${app.rabbitmq.catalogo.dlq}")
    public void registrarDlqCatalogo(Message mensaje, Channel channel) throws IOException {
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Object motivo = mensaje.getMessageProperties().getHeader("x-death");
        log.warn("[DLQ CATALOGO LOG] Mensaje no entregado registrado en DLQ. cuerpo={} x-death={}", cuerpo, motivo);
        channel.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
    }
}
