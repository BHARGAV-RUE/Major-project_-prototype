package com.bhargav.cloudtransfer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int NONCE_LENGTH = 12; // 96-bit nonce is standard for ChaCha20-Poly1305

    /**
     * Generates a single master SecretKey for the entire file transfer.
     * In a production system, this key might be derived from a user password or shared via ECDH.
     */
    public SecretKey generateTransferKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("ChaCha20");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    /**
     * Encrypts a single file chunk using ChaCha20-Poly1305.
     * Each file part is encrypted separately, using a randomly generated IV.
     *
     * @param plaintext The raw byte array from the ChunkingService
     * @param key       The master secret key for this transfer
     * @return EncryptedChunkPayload containing the ciphertext and the unique nonce
     */
    public EncryptedChunkPayload encryptChunk(byte[] plaintext, SecretKey key) throws Exception {
        // Generate a random 12-byte IV (nonce) for this specific chunk
        byte[] nonce = new byte[NONCE_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        log.debug("Chunk encrypted successfully. Ciphertext size: {}", ciphertext.length);
        return new EncryptedChunkPayload(ciphertext, nonce);
    }

    /**
     * Decrypts a single file chunk back to its original bytes.
     *
     * @param ciphertext The encrypted byte array downloaded from the Cloud Provider
     * @param key        The master secret key for this transfer
     * @param nonce      The exact IV that was used to encrypt this chunk
     * @return The decrypted original bytes
     */
    public byte[] decryptChunk(byte[] ciphertext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] plaintext = cipher.doFinal(ciphertext);

        log.debug("Chunk decrypted successfully. Plaintext size: {}", plaintext.length);
        return plaintext;
    }
}