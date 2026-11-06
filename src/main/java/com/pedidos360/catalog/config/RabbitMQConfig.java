package com.pedidos360.catalog.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada de RabbitMQ para el dominio funcional de Inventario / Stock.
 * Todos los nombres de exchanges, colas y routing keys se inyectan desde application.yml
 * (app.rabbitmq.catalogo.*), manteniendo cero strings acoplados y separando la infraestructura
 * de la lógica de negocio.
 *
 * <pre>
 * Caso de uso: Auditoría y alertas asíncronas de actualización de stock
 *
 *   CatalogStockProducer --routing-key--> [catalogo.exchange (direct)] --binding--> [catalogo.stock.queue]
 *                                                                                     |  (NACK requeue=false)
 *                                                                                     v  dead-letter-exchange + key
 *                                         [catalogo.dlx (direct)] -----binding----> [catalogo.stock.dlq]
 *
 *   Consumidor principal : messaging.inventario.CatalogStockConsumer (ACK manual, reintento y envío a DLQ)
 *   Consumidor DLQ       : messaging.inventario.CatalogStockDlqConsumer (registro en logs y ACK)
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.catalogo.exchange}")
    private String catalogoExchange;

    @Value("${app.rabbitmq.catalogo.queue}")
    private String catalogoQueue;

    @Value("${app.rabbitmq.catalogo.routing-key}")
    private String catalogoRoutingKey;

    @Value("${app.rabbitmq.catalogo.dlx}")
    private String dlxExchange;

    @Value("${app.rabbitmq.catalogo.dlq}")
    private String dlqQueue;

    @Value("${app.rabbitmq.catalogo.dlq-routing-key}")
    private String dlqRoutingKey;

    @Bean
    public DirectExchange catalogoDeadLetterExchange() {
        return new DirectExchange(dlxExchange);
    }

    @Bean
    public Queue catalogoDeadLetterQueue() {
        return QueueBuilder.durable(dlqQueue).build();
    }

    @Bean
    public Binding catalogoDlqBinding() {
        return BindingBuilder.bind(catalogoDeadLetterQueue())
                .to(catalogoDeadLetterExchange())
                .with(dlqRoutingKey);
    }

    @Bean
    public DirectExchange catalogoExchange() {
        return new DirectExchange(catalogoExchange);
    }

    @Bean
    public Queue catalogoQueue() {
        return QueueBuilder.durable(catalogoQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }

    @Bean
    public Binding catalogoBinding() {
        return BindingBuilder.bind(catalogoQueue())
                .to(catalogoExchange())
                .with(catalogoRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
