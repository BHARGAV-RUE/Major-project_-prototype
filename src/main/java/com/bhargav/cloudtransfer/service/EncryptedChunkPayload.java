package com.bhargav.cloudtransfer.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EncryptedChunkPayload {
    private byte[] ciphertext; // The encrypted data
    private byte[] nonce;      // The randomly generated IV needed for decryption
}