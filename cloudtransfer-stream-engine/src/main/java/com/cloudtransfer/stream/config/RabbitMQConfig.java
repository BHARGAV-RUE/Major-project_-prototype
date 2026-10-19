package com.cloudtransfer.stream.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_INIT_QUEUE = "upload-init-queue";
    public static final String CHUNK_HASH_QUEUE = "chunk-hash-queue";
    public static final String CHUNK_HASH_EXCHANGE = "chunk-hash-exchange";
    public static final String CHUNK_HASH_ROUTING_KEY = "chunk.hash.key";

    // --- Added for Brain -> Muscle File Command ---
    public static final String FILE_PROCESS_QUEUE = "file-process-queue";
    public static final String EXCHANGE = "cloudtransfer-exchange";
    public static final String FILE_PROCESS_ROUTING_KEY = "file.process";

    @Bean
    public Queue chunkHashQueue() {
        return new Queue(CHUNK_HASH_QUEUE, true);
    }

    @Bean
    public TopicExchange chunkHashExchange() {
        return new TopicExchange(CHUNK_HASH_EXCHANGE);
    }

    @Bean
    public Binding chunkHashBinding(Queue chunkHashQueue, TopicExchange chunkHashExchange) {
        return BindingBuilder.bind(chunkHashQueue).to(chunkHashExchange).with(CHUNK_HASH_ROUTING_KEY);
    }

    // --- New Beans for File Processing ---
    @Bean
    public Queue fileProcessQueue() {
        return new Queue(FILE_PROCESS_QUEUE, true);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding fileProcessBinding(Queue fileProcessQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(fileProcessQueue).to(directExchange).with(FILE_PROCESS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}