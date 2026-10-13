package com.cloudtransfer.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Capacity Limit Exceeded");
        errorResponse.put("message", ex.getMessage());

        // Returns a clean 400 Bad Request instead of a scary 500 crash
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.amqp.AmqpConnectException.class)
    public ResponseEntity<Map<String, String>> handleRabbitMQConnection(org.springframework.amqp.AmqpConnectException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Message Broker Unavailable");
        errorResponse.put("message", "Could not connect to RabbitMQ. Ensure the service is running.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
}