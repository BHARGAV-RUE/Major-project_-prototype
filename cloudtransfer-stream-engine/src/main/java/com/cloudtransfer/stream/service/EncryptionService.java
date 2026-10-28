package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.exception.ChunkProcessingException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "ChaCha20";
    private static final String PROTOTYPE_MASTER_KEY = "CloudTransferSuperSecretKey2026!";
    private static final int NONCE_SIZE_BYTES = 12; // 96-bit nonce for ChaCha20

    private SecretKeySpec get256BitKey() throws Exception {
        byte[] keyBytes = PROTOTYPE_MASTER_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hashedKey = sha256.digest(keyBytes);
        return new SecretKeySpec(hashedKey, ALGORITHM);
    }

    public byte[] encrypt(byte[] rawChunkData) {
        return encryptChunk(rawChunkData);
    }

    public byte[] encryptChunk(byte[] rawChunkData, String sessionId, Integer chunkIndex) {
        return encryptChunk(rawChunkData);
    }

    /**
     * Encrypts file chunk using ChaCha20 stream cipher.
     * Prepends the 12-byte Nonce to the encrypted payload for decryption later.
     */
    public byte[] encryptChunk(byte[] rawChunkData) {
        try {
            byte[] nonce = new byte[NONCE_SIZE_BYTES];
            SecureRandom random = new SecureRandom();
            random.nextBytes(nonce);

            ChaCha20ParameterSpec paramSpec = new ChaCha20ParameterSpec(nonce, 1);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, get256BitKey(), paramSpec);

            byte[] encryptedData = cipher.doFinal(rawChunkData);

            byte[] combinedPayload = new byte[NONCE_SIZE_BYTES + encryptedData.length];
            System.arraycopy(nonce, 0, combinedPayload, 0, NONCE_SIZE_BYTES);
            System.arraycopy(encryptedData, 0, combinedPayload, NONCE_SIZE_BYTES, encryptedData.length);

            return combinedPayload;

        } catch (Exception e) {
            throw new ChunkProcessingException("ChaCha20 stream encryption failed", e);
        }
    }

    public byte[] decryptChunk(byte[] combinedPayload, String sessionId, Integer chunkIndex) {
        return decryptChunk(combinedPayload);
    }

    /**
     * Decrypts a ChaCha20 encrypted payload.
     */
    public byte[] decryptChunk(byte[] combinedPayload) {
        try {
            byte[] nonce = Arrays.copyOfRange(combinedPayload, 0, NONCE_SIZE_BYTES);
            byte[] encryptedData = Arrays.copyOfRange(combinedPayload, NONCE_SIZE_BYTES, combinedPayload.length);

            ChaCha20ParameterSpec paramSpec = new ChaCha20ParameterSpec(nonce, 1);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, get256BitKey(), paramSpec);

            return cipher.doFinal(encryptedData);

        } catch (Exception e) {
            throw new ChunkProcessingException("ChaCha20 stream decryption failed", e);
        }
    }

    /**
     * Computes the SHA-256 hash of an encrypted chunk.
     */
    public String calculateSha256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ChunkProcessingException("SHA-256 hashing failed during chunk verification", e);
        }
    }
}