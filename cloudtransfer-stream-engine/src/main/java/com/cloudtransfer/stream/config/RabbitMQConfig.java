package com.cloudtransfer.stream.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_INIT_QUEUE = "upload-init-queue";
    public static final String CHUNK_HASH_QUEUE = "chunk-hash-queue";
    public static final String CHUNK_HASH_EXCHANGE = "chunk-hash-exchange";
    public static final String CHUNK_HASH_ROUTING_KEY = "chunk.hash.key";

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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}