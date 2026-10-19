package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.config.RabbitMQConfig;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendFileForProcessing(String fileId, String filename, String filePath, List<CloudProviderTokenDTO> tokens) {
        FileProcessCommandMessage message = new FileProcessCommandMessage(
                fileId,
                filename,
                filePath,
                tokens
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message
        );

        log.info("[Brain] Sent FileProcessCommandMessage to Muscle for file: {}", filename);
    }
}