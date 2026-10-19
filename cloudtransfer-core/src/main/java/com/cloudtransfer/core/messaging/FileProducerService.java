package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.config.RabbitMQConfig;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileProducerService {

    private final RabbitTemplate rabbitTemplate;

    public FileProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendFileForProcessing(String fileId, String fileName, String localFilePath, List<CloudProviderTokenDTO> tokens) {
        FileProcessCommandMessage message = new FileProcessCommandMessage(fileId, fileName, localFilePath, tokens);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.FILE_PROCESS_ROUTING_KEY,
                message
        );
        System.out.println(" [Brain] Sent FileProcessCommandMessage to Muscle for file: " + fileName);
    }
}