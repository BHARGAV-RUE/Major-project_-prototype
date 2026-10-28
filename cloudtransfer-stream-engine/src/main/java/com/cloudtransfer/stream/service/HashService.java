package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.exception.ChunkProcessingException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {

    public String calculateSha256(byte[] data) {
        return calculateSHA256(data);
    }

    /**
     * Generates a SHA-256 hash of the encrypted chunk payload.
     * Returns the hash as a hexadecimal string.
     */
    public String calculateSHA256(byte[] encryptedData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(encryptedData);

            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new ChunkProcessingException("Failed to generate SHA-256 chunk hash", e);
        }
    }
}