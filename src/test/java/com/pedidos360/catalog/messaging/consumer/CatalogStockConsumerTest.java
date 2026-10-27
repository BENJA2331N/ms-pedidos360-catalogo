package com.pedidos360.catalog.messaging.consumer;

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
    private final Channel channel = mock(Channel.class);

    @Test
    @DisplayName("Evento de stock válido -> ACK explícito")
    void validEventIsAcked() throws IOException {
        consumer.consumirEventoStock(new StockUpdateDTO(3), channel, 1L, false);

        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Cantidad inválida -> NACK sin requeue (va a la DLQ)")
    void invalidQuantityGoesToDlq() throws IOException {
        consumer.consumirEventoStock(new StockUpdateDTO(0), channel, 2L, false);

        verify(channel).basicNack(2L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Evento nulo -> NACK sin requeue (va a la DLQ)")
    void nullEventGoesToDlq() throws IOException {
        consumer.consumirEventoStock(null, channel, 3L, null);

        verify(channel).basicNack(3L, false, false);
    }

    @Test
    @DisplayName("Mensaje en la DLQ -> se registra y se confirma (ACK)")
    void dlqMessageIsLoggedAndAcked() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(4L);
        Message message = new Message("no-es-json".getBytes(StandardCharsets.UTF_8), props);

        consumer.registrarDlqCatalogo(message, channel);

        verify(channel).basicAck(4L, false);
    }
}
