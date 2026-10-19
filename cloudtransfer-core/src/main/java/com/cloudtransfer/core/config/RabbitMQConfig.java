package com.cloudtransfer.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_INIT_QUEUE = "upload-init-queue";
    public static final String EXCHANGE = "cloudtransfer-exchange";
    public static final String ROUTING_KEY = "upload.init";
    public static final String CHUNK_HASH_QUEUE = "chunk-hash-queue";
    public static final String HASH_ROUTING_KEY = "chunk.hash";

    // --- Added for Brain -> Muscle File Command ---
    public static final String FILE_PROCESS_QUEUE = "file-process-queue";
    public static final String FILE_PROCESS_ROUTING_KEY = "file.process";

    @Bean
    public Queue uploadInitQueue() {
        return new Queue(UPLOAD_INIT_QUEUE, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue uploadInitQueue, DirectExchange exchange) {
        return BindingBuilder.bind(uploadInitQueue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }
    @Bean
    public Queue chunkHashQueue() {
        return new Queue(CHUNK_HASH_QUEUE, true);
    }

    @Bean
    public Binding hashBinding(Queue chunkHashQueue, DirectExchange exchange) {
        return BindingBuilder.bind(chunkHashQueue).to(exchange).with(HASH_ROUTING_KEY);
    }

    // --- New Beans for File Processing ---
    @Bean
    public Queue fileProcessQueue() {
        return new Queue(FILE_PROCESS_QUEUE, true);
    }

    @Bean
    public Binding fileProcessBinding(Queue fileProcessQueue, DirectExchange exchange) {
        return BindingBuilder.bind(fileProcessQueue).to(exchange).with(FILE_PROCESS_ROUTING_KEY);
    }
}