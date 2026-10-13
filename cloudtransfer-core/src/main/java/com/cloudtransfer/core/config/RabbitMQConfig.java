package com.cloudtransfer.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_INIT_QUEUE = "upload-init-queue";
    public static final String EXCHANGE = "cloudtransfer-exchange";
    public static final String ROUTING_KEY = "upload.init";

    @Bean
    public Queue uploadInitQueue() {
        return new Queue(UPLOAD_INIT_QUEUE, true); // true = durable (survives server restarts)
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue uploadInitQueue, DirectExchange exchange) {
        return BindingBuilder.bind(uploadInitQueue).to(exchange).with(ROUTING_KEY);
    }

    // Converts our Java DTOs into JSON before sending them to RabbitMQ
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}