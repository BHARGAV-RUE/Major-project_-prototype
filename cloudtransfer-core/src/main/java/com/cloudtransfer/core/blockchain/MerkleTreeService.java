package com.cloudtransfer.core.blockchain;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Service
public class MerkleTreeService {

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
                // If there is an odd number of hashes, duplicate the last one (Standard Merkle protocol)
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
            String combined = left + right; // Concatenate the two hex strings
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