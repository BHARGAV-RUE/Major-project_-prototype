package com.cloudtransfer.stream.controller;

import com.cloudtransfer.stream.dto.ChunkHashMessage;
import com.cloudtransfer.stream.service.EncryptionService;
import com.cloudtransfer.stream.service.HashService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamController {

    private final EncryptionService encryptionService;
    private final HashService hashService;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/upload-chunk")
    public ResponseEntity<Map<String, String>> receiveAndEncryptChunk(
            @RequestParam("sessionId") UUID sessionId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("file") MultipartFile chunkFile) throws IOException {

        System.out.println("--- NEW CHUNK RECEIVED ---");
        System.out.println("Session ID: " + sessionId + " | Chunk Index: " + chunkIndex);

        byte[] rawBytes = chunkFile.getBytes();
        byte[] encryptedBytes = encryptionService.encryptChunk(rawBytes);
        String chunkHash = hashService.calculateSHA256(encryptedBytes);

        // SEND HASH BACK TO THE BRAIN
        ChunkHashMessage hashMessage = new ChunkHashMessage(sessionId, chunkIndex, chunkHash);
        rabbitTemplate.convertAndSend("cloudtransfer-exchange", "chunk.hash", hashMessage);

        System.out.println("Encrypted Size: " + encryptedBytes.length + " bytes");
        System.out.println("Chunk SHA-256 Hash: " + chunkHash);
        System.out.println("Transmitted Hash to Brain via RabbitMQ.");
        System.out.println("--------------------------");

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "chunkIndex", chunkIndex.toString(),
                "chunkHash", chunkHash
        ));
    }
}