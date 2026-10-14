package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.blockchain.MerkleTreeService;
import com.cloudtransfer.core.config.RabbitMQConfig;
import com.cloudtransfer.core.dto.ChunkHashMessage;
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

    // Temporary in-memory store for session hashes (in production, this queries PostgreSQL)
    private final ConcurrentHashMap<String, List<String>> sessionHashes = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.CHUNK_HASH_QUEUE)
    public void handleChunkHash(ChunkHashMessage message) {
        String sessionId = message.getSessionId().toString();

        System.out.println("BRAIN RECEIVED HASH FOR CHUNK " + message.getChunkIndex());

        // Store the hash
        sessionHashes.putIfAbsent(sessionId, new ArrayList<>());
        sessionHashes.get(sessionId).add(message.getChunkHash());

        // For testing: If we receive 10 hashes, generate the Merkle Root!
        // (You can adjust this number based on your Postman test payload)
        if (sessionHashes.get(sessionId).size() == 10) {
            System.out.println("All chunks received for Session: " + sessionId);
            String merkleRoot = merkleTreeService.generateMerkleRoot(sessionHashes.get(sessionId));
            System.out.println("==================================================");
            System.out.println("MERKLE ROOT GENERATED: " + merkleRoot);
            System.out.println("Ready for Blockchain Anchoring!");
            System.out.println("==================================================");

            // Clean up memory
            sessionHashes.remove(sessionId);
        }
    }
}