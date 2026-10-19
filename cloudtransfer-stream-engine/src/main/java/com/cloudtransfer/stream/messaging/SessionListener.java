package com.cloudtransfer.stream.messaging;

import com.cloudtransfer.stream.config.RabbitMQConfig;
import com.cloudtransfer.stream.dto.SessionInitMessage;
import com.cloudtransfer.stream.dto.FileProcessCommandMessage;
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

    // --- Added for Brain -> Muscle File Command ---
    @RabbitListener(queues = RabbitMQConfig.FILE_PROCESS_QUEUE)
    public void receiveFileCommand(FileProcessCommandMessage message) {
        System.out.println("\n--------------------------------------------------");
        System.out.println(" [Muscle] Received command for file: " + message.getFileName());
        System.out.println(" [Muscle] File ID: " + message.getFileId());
        System.out.println(" [Muscle] Local File Path: " + message.getLocalFilePath());

        if (message.getVolunteerTokens() != null) {
            System.out.println(" [Muscle] Received " + message.getVolunteerTokens().size() + " OAuth tokens:");
            message.getVolunteerTokens().forEach(token -> {
                System.out.println("   -> Provider: " + token.getProviderName() +
                        " | Volunteer: " + token.getVolunteerEmail() +
                        " | Token: " + token.getRefreshToken());
            });
        } else {
            System.out.println(" [Muscle] WARNING: No OAuth tokens received!");
        }
        System.out.println("--------------------------------------------------\n");

        // Muscle is now ready to take these tokens and start chunking & uploading!
    }
}