package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.blockchain.MerkleTreeService; // Fixed import
import com.cloudtransfer.core.config.RabbitMQConfig;
import com.cloudtransfer.core.dto.ChunkHashMessage;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChunkHashListener {

    private final MerkleTreeService merkleTreeService;
    private final FileChunkRepository fileChunkRepository;
    private final TransferSessionRepository transferSessionRepository;

    private final ConcurrentHashMap<String, List<String>> sessionHashes = new ConcurrentHashMap<>();

    private static final long CHUNK_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

    @RabbitListener(queues = RabbitMQConfig.CHUNK_HASH_QUEUE)
    public void handleChunkHash(ChunkHashMessage message) {
        String sessionId = message.getSessionId().toString();

        log.info("[Brain] Received Chunk #{} | Hash: {} | Cloud ID: {} | Provider: {}",
                message.getChunkIndex(), message.getChunkHash(), message.getCloudFileId(), message.getProviderName());

        TransferSession session;
        try {
            session = transferSessionRepository.findById(message.getSessionId())
                    .orElseGet(() -> {
                        TransferSession newSession = new TransferSession();
                        newSession.setId(message.getSessionId());
                        newSession.setExpiresAt(LocalDateTime.now().plusDays(1));
                        newSession.setOriginalFilename("postman_test_file.bin");
                        newSession.setSenderEmail("tester@cloudtransfer.local");
                        newSession.setTotalSizeBytes(15728640L); // 15MB default test size

                        return transferSessionRepository.save(newSession);
                    });

            FileChunk chunk = new FileChunk();
            chunk.setSession(session);
            chunk.setChunkIndex(message.getChunkIndex());
            chunk.setChunkHash(message.getChunkHash());
            chunk.setCloudFileId(message.getCloudFileId());
            chunk.setProviderName(message.getProviderName());

            fileChunkRepository.save(chunk);
            log.info("[Brain] Saved FileChunk #{} to PostgreSQL", message.getChunkIndex());
        } catch (Exception e) {
            log.error("Could not save FileChunk to DB: {}", e.getMessage());
            return;
        }

        // Accumulate chunk hashes for Merkle Tree construction
        sessionHashes.putIfAbsent(sessionId, new ArrayList<>());
        sessionHashes.get(sessionId).add(message.getChunkHash());

        // Calculate total expected chunks dynamically based on session file size
        long totalBytes = (session.getTotalSizeBytes() != null && session.getTotalSizeBytes() > 0) ? session.getTotalSizeBytes() : CHUNK_SIZE_BYTES;
        int expectedChunks = (int) Math.ceil((double) totalBytes / CHUNK_SIZE_BYTES);

        // Once all chunks arrive, construct Merkle Tree and trigger Blockchain Logging
        if (sessionHashes.get(sessionId).size() >= expectedChunks) {
            log.info("All {} expected chunk hashes received for Session: {}", expectedChunks, sessionId);

            merkleTreeService.processAllChunks(sessionId, sessionHashes.get(sessionId));

            sessionHashes.remove(sessionId);
        }
    }
}