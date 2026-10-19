package com.cloudtransfer.stream.messaging;

import com.cloudtransfer.stream.config.RabbitMQConfig;
import com.cloudtransfer.stream.dto.FileProcessCommandMessage; // <-- Added missing import
import com.cloudtransfer.stream.service.FileChunkProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionListener {

    private final FileChunkProcessorService fileChunkProcessorService;

    @RabbitListener(queues = RabbitMQConfig.FILE_PROCESS_QUEUE)
    public void handleFileProcessCommand(FileProcessCommandMessage message) {
        log.info("--------------------------------------------------");
        log.info("[Muscle] SessionListener received file command: {}", message.getFilename());
        log.info("[Muscle] Session ID: {}", message.getFileId());
        log.info("[Muscle] Path: {}", message.getFilePath());
        log.info("[Muscle] Volunteers count: {}", message.getTokens() != null ? message.getTokens().size() : 0);
        log.info("--------------------------------------------------");

        fileChunkProcessorService.processAndUploadFile(
                message.getFileId(),
                message.getFilename(),
                message.getFilePath(),
                message.getTokens()
        );
    }
}