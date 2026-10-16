package com.cloudtransfer.stream.controller;

import com.cloudtransfer.stream.config.RabbitMQConfig;
import com.cloudtransfer.stream.dto.ChunkHashMessage;
import com.cloudtransfer.stream.service.EncryptionService;
import com.cloudtransfer.stream.storage.MultiCloudRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamController {

    private final EncryptionService encryptionService;
    private final MultiCloudRouter multiCloudRouter;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/upload-chunk")
    public ResponseEntity<String> uploadChunk(
            @RequestParam("sessionId") UUID sessionId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("file") MultipartFile file) {

        try {
            byte[] rawBytes = file.getBytes();

            // 1. Encrypt raw chunk bytes
            // Note: Check EncryptionService.java if your method is named encryptChunk() or encrypt()
            byte[] encryptedData = encryptionService.encrypt(rawBytes);

            // 2. Compute SHA-256 hash of encrypted chunk
            String chunkHash = computeSha256(encryptedData);

            // 3. Route chunk to cloud (Google Drive / Dropbox)
            String cloudFileId = multiCloudRouter.routeChunkToCloud(sessionId.toString(), chunkIndex, encryptedData);

            // 4. Send metadata + cloudFileId to Brain over RabbitMQ
            ChunkHashMessage message = new ChunkHashMessage(
                    sessionId,
                    chunkIndex,
                    chunkHash,
                    cloudFileId,
                    "MULTI_CLOUD"
            );

            // Check RabbitMQConfig.java for your exact constant names:
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHUNK_HASH_QUEUE,
                    message
            );

            return ResponseEntity.ok("Chunk " + chunkIndex + " routed to cloud and hash queued successfully.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process chunk: " + e.getMessage());
        }
    }

    private String computeSha256(byte[] data) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}