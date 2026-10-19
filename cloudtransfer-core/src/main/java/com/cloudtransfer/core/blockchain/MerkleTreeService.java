package com.cloudtransfer.core.blockchain;

import com.cloudtransfer.core.service.BlockchainService;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Service
public class MerkleTreeService {

    private final BlockchainService blockchainService;

    public MerkleTreeService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    /**
     * Processes collected chunk hashes, generates the Merkle Root, and triggers blockchain anchoring.
     */
    public String processAllChunks(String sessionId, List<String> chunkHashes) {
        // 1. Calculate the real SHA-256 Merkle Root
        String root = generateMerkleRoot(chunkHashes);

        System.out.println("==================================================");
        System.out.println("MERKLE ROOT GENERATED: " + root);
        System.out.println("Ready for Blockchain Anchoring!");
        System.out.println("==================================================");

        // 2. Trigger the blockchain anchoring service
        blockchainService.anchorRootToBlockchain(sessionId, root);

        return root;
    }

    /**
     * Takes a list of individual chunk SHA-256 hashes and reduces them to a single Merkle Root.
     */
    public String generateMerkleRoot(List<String> chunkHashes) {
        if (chunkHashes == null || chunkHashes.isEmpty()) {
            throw new IllegalArgumentException("Chunk hashes cannot be empty");
        }

        List<String> currentLevel = new ArrayList<>(chunkHashes);

        // Continue hashing pairs until only 1 root hash remains
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                // Duplicate last hash if odd number of elements (Standard Merkle protocol)
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                nextLevel.add(hashPair(left, right));
            }
            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }

    private String hashPair(String left, String right) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = left + right;
            byte[] hashBytes = digest.digest(combined.getBytes());

            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to hash Merkle pair", e);
        }
    }
}