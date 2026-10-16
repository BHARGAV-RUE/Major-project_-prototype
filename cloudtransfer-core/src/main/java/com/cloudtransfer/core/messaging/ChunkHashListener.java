package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.dto.ChunkHashMessage;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import com.cloudtransfer.core.service.MerkleTreeService;
import com.cloudtransfer.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChunkHashListener {

    private final MerkleTreeService merkleTreeService;
    private final FileChunkRepository fileChunkRepository;
    private final TransferSessionRepository transferSessionRepository; // <--- Added Repository

    private final ConcurrentHashMap<String, List<String>> sessionHashes = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.CHUNK_HASH_QUEUE)
    public void handleChunkHash(ChunkHashMessage message) {
        String sessionId = message.getSessionId().toString();

        System.out.println("BRAIN RECEIVED CHUNK #" + message.getChunkIndex() +
                " | Hash: " + message.getChunkHash() +
                " | Cloud ID: " + message.getCloudFileId() +
                " | Provider: " + message.getProviderName());

        // 1. Save metadata to PostgreSQL via FileChunkRepository
        // 1. Save metadata to PostgreSQL via FileChunkRepository
        try {
            // Find the existing session, or create a mock one with required fields for Postman testing
            TransferSession session = transferSessionRepository.findById(message.getSessionId())
                    .orElseGet(() -> {
                        TransferSession newSession = new TransferSession();
                        newSession.setId(message.getSessionId());

                        // Populate required fields to satisfy NOT NULL database constraints
                        newSession.setExpiresAt(java.time.LocalDateTime.now().plusDays(1));
                        newSession.setOriginalFilename("postman_test_file.bin");
                        newSession.setSenderEmail("tester@cloudtransfer.local");
                        newSession.setTotalSizeBytes(1024L);

                        return transferSessionRepository.save(newSession);
                    });

            FileChunk chunk = new FileChunk();
            chunk.setSession(session);
            chunk.setChunkIndex(message.getChunkIndex());
            chunk.setChunkHash(message.getChunkHash());
            chunk.setCloudFileId(message.getCloudFileId());
            chunk.setProviderName(message.getProviderName());

            fileChunkRepository.save(chunk);
        } catch (Exception e) {
            System.err.println("Could not save FileChunk to DB: " + e.getMessage());
        }

        // 2. Accumulate hashes for Merkle Tree
        sessionHashes.putIfAbsent(sessionId, new ArrayList<>());
        sessionHashes.get(sessionId).add(message.getChunkHash());

        // 3. Once 10 chunks arrive, calculate Merkle Root and trigger Blockchain
        if (sessionHashes.get(sessionId).size() == 10) {
            System.out.println("All 10 chunks received for Session: " + sessionId);

            merkleTreeService.processAllChunks(sessionId, sessionHashes.get(sessionId));

            sessionHashes.remove(sessionId);
        }
    }
}