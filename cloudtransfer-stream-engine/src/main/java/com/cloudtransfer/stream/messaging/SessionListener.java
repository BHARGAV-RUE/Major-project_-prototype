package com.cloudtransfer.stream.messaging;

import com.cloudtransfer.stream.config.RabbitMQConfig;
import com.cloudtransfer.stream.dto.SessionInitMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SessionListener {

    @RabbitListener(queues = RabbitMQConfig.UPLOAD_INIT_QUEUE)
    public void handleSessionInit(SessionInitMessage message) {
        System.out.println("==================================================");
        System.out.println("MUSCLE RECEIVED NEW UPLOAD ORDER!");
        System.out.println("Session ID: " + message.getSessionId());
        System.out.println("Expecting Chunks: " + message.getTotalChunks());
        System.out.println("Ready to begin receiving file streams...");
        System.out.println("==================================================");

        // In the next phase, we will initialize the encryption streams here
    }
}