package com.pedidos360.catalog.messaging.inventario;

import com.pedidos360.catalog.dto.StockUpdateDTO;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

class CatalogStockConsumerTest {

    private final CatalogStockConsumer consumer = new CatalogStockConsumer();
    private final CatalogStockDlqConsumer dlqConsumer = new CatalogStockDlqConsumer();
    private final Channel channel = mock(Channel.class);

    @Test
    @DisplayName("Evento de stock válido -> ACK explícito")
    void validEventIsAcked() throws IOException {
        consumer.consumirEventoStock(new StockUpdateDTO(1L, "Monitor", 3, 4), channel, 1L, false);

        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Cantidad inválida -> NACK sin requeue (envío directo a DLQ)")
    void invalidQuantityGoesToDlq() throws IOException {
        consumer.consumirEventoStock(new StockUpdateDTO(0), channel, 2L, false);

        verify(channel).basicNack(2L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Error transitorio en primer intento -> NACK con requeue=true (reintento)")
    void transientErrorIsRequeuedOnce() throws IOException {
        StockUpdateDTO dto = new StockUpdateDTO(1L, "Teclado", 2, 10);
        dto.setMotivo("RETRY");

        consumer.consumirEventoStock(dto, channel, 3L, false);

        verify(channel).basicNack(3L, false, true);
    }

    @Test
    @DisplayName("Error transitorio ya reentregado -> NACK con requeue=false (va a la DLQ)")
    void redeliveredTransientErrorGoesToDlq() throws IOException {
        StockUpdateDTO dto = new StockUpdateDTO(1L, "Teclado", 2, 10);
        dto.setMotivo("RETRY");

        consumer.consumirEventoStock(dto, channel, 4L, true);

        verify(channel).basicNack(4L, false, false);
    }

    @Test
    @DisplayName("JSON malformado en Message -> NACK sin requeue hacia DLQ")
    void malformedJsonGoesToDlq() throws IOException {
        MessageProperties props = new MessageProperties();
        Message message = new Message("{malformed-json".getBytes(StandardCharsets.UTF_8), props);

        consumer.recibirMensajeStock(message, channel, 5L, false);

        verify(channel).basicNack(5L, false, false);
    }

    @Test
    @DisplayName("Mensaje en la DLQ -> se registra en logs y se confirma (ACK)")
    void dlqMessageIsLoggedAndAcked() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(6L);
        Message message = new Message("no-es-json".getBytes(StandardCharsets.UTF_8), props);

        dlqConsumer.registrarDlqCatalogo(message, channel);

        verify(channel).basicAck(6L, false);
    }
}
