package com.pedidos360.catalog.messaging.inventario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Consumidor del dominio funcional "Inventario / Stock" con ACK manual explícito
 * ({@code spring.rabbitmq.listener.simple.acknowledge-mode: manual}).
 *
 * <h3>Política documentada de ACK, reintento y envío a DLQ</h3>
 * <ul>
 *   <li><b>Ruta 1 — Éxito ({@code basicAck})</b>: el evento de stock es válido, se registra el
 *       movimiento (y alerta de stock bajo si corresponde) y se confirma explícitamente.</li>
 *   <li><b>Ruta 2 — Error no recuperable ({@code basicNack(requeue=false)} -> DLQ)</b>: JSON malformado
 *       ({@link JsonProcessingException}), payload nulo o cantidad menor/igual a cero
 *       ({@link IllegalArgumentException}). Se rechaza sin reencolar para que RabbitMQ lo enrute
 *       inmediatamente al Dead Letter Exchange ({@code app.rabbitmq.catalogo.dlx}).</li>
 *   <li><b>Ruta 3 — Error transitorio / recuperable ({@code basicNack(requeue=true)} con límite)</b>:
 *       ante un fallo temporal (ej. motivo {@code RETRY} o excepción inesperada en la primera entrega,
 *       {@code redelivered == false}), se reintenta reencolando una vez. Si falla nuevamente tras haber
 *       sido reentregado ({@code redelivered == true}), se envía a la DLQ con {@code basicNack(requeue=false)}.</li>
 * </ul>
 */
@Component
public class CatalogStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogStockConsumer.class);
    private static final int UMBRAL_STOCK_BAJO = 5;

    private final ObjectMapper objectMapper;

    public CatalogStockConsumer() {
        this.objectMapper = new ObjectMapper();
    }

    public CatalogStockConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${app.rabbitmq.catalogo.queue}")
    public void recibirMensajeStock(Message message,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                    @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered)
            throws IOException {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            log.error("[INVENTARIO] Mensaje vacío recibido. Rechazando (NACK sin requeue) hacia DLQ.");
            channel.basicNack(tag, false, false);
            return;
        }
        String rawJson = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            StockUpdateDTO dto = objectMapper.readValue(rawJson, StockUpdateDTO.class);
            consumirEventoStock(dto, channel, tag, redelivered);
        } catch (JsonProcessingException ex) {
            log.error("[INVENTARIO] JSON inválido no recuperable ({}). Enviando a DLQ (NACK sin requeue): payload={}",
                    ex.getOriginalMessage(), rawJson);
            channel.basicNack(tag, false, false);
        }
    }

    public void consumirEventoStock(StockUpdateDTO dto,
                                    Channel channel,
                                    long tag,
                                    Boolean redelivered) throws IOException {
        try {
            log.info("[INVENTARIO] Procesando evento asíncrono de stock: {}", dto);

            if (dto == null) {
                throw new IllegalArgumentException("Datos de stock nulos");
            }
            if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cantidad de stock inválida: " + dto.getCantidad());
            }
            if (dto.getMotivo() != null && dto.getMotivo().toUpperCase().contains("FORCE_DLQ")) {
                throw new IllegalArgumentException("Error forzado hacia DLQ: " + dto.getMotivo());
            }
            if (dto.getMotivo() != null && dto.getMotivo().toUpperCase().contains("RETRY")) {
                throw new IllegalStateException("Fallo transitorio simulado en auditoría de inventario");
            }

            if (dto.getStockRestante() != null && dto.getStockRestante() < UMBRAL_STOCK_BAJO) {
                log.warn("[INVENTARIO] ALERTA DE STOCK CRÍTICO para producto ID {} ('{}'): quedan {} unidades.",
                        dto.getProductId(), dto.getProductoNombre(), dto.getStockRestante());
            }

            channel.basicAck(tag, false);
            log.info("[INVENTARIO] Evento de stock confirmado explícitamente (ACK). tag={}", tag);

        } catch (IllegalArgumentException ex) {
            log.error("[INVENTARIO] Error de validación no recuperable. Enviando a DLQ (NACK requeue=false): {}",
                    ex.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            if (!Boolean.TRUE.equals(redelivered)) {
                log.warn("[INVENTARIO] Error transitorio en primer intento. Reencolando para reintento (NACK requeue=true): {}",
                        ex.getMessage());
                channel.basicNack(tag, false, true);
            } else {
                log.error("[INVENTARIO] Reintentos agotados tras fallo persistente. Enviando a DLQ (NACK requeue=false): {}",
                        ex.getMessage());
                channel.basicNack(tag, false, false);
            }
        }
    }
}
