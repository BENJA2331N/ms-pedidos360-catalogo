package com.pedidos360.catalog.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
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
