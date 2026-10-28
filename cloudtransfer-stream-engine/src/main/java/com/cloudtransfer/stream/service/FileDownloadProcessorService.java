package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.dto.CloudProviderTokenDTO;
import com.cloudtransfer.stream.storage.MultiCloudRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileDownloadProcessorService {

    private final MultiCloudRouter multiCloudRouter;
    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cloudtransfer.core.url:http://localhost:8080/api/v1}")
    private String coreEngineBaseUrl;

    public byte[] downloadAndDecryptChunk(String sessionId, Integer chunkIndex) throws Exception {
        log.info("[FileDownloadProcessor] Processing download for session: {}, chunkIndex: {}", sessionId, chunkIndex);

        // 1. Fetch chunk assignment metadata from Core Engine
        String coreUrl = coreEngineBaseUrl + "/transfers/" + sessionId + "/chunk-assignments";
        ResponseEntity<List<CloudProviderTokenDTO>> response = restTemplate.exchange(
                coreUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CloudProviderTokenDTO>>() {}
        );

        List<CloudProviderTokenDTO> tokenList = response.getBody();
        if (tokenList == null || tokenList.isEmpty()) {
            throw new IllegalStateException("No chunk assignment metadata found for session " + sessionId);
        }

        // Determine the maximum chunk index for this transfer session
        int maxChunkIndex = tokenList.stream()
                .mapToInt(CloudProviderTokenDTO::getChunkIndex)
                .max()
                .orElse(0);

        // Locate Primary and Replica tokens for this specific chunk index
        CloudProviderTokenDTO primaryToken = tokenList.stream()
                .filter(t -> t.getChunkIndex() == chunkIndex && !t.isReplica())
                .findFirst()
                .orElse(null);

        CloudProviderTokenDTO replicaToken = tokenList.stream()
                .filter(t -> t.getChunkIndex() == chunkIndex && t.isReplica())
                .findFirst()
                .orElse(null);

        byte[] encryptedBytes = null;
        byte[] decryptedBytes = null;

        // --- STEP 1: Attempt Primary Download ---
        if (primaryToken != null) {
            try {
                log.info("[FileDownloadProcessor] Attempting PRIMARY download from [{}] for Chunk #{}",
                        primaryToken.getProviderName(), chunkIndex);

                encryptedBytes = multiCloudRouter.downloadChunkFromCloud(
                        primaryToken.getProviderName(),
                        primaryToken.getRefreshToken(),
                        sessionId,
                        chunkIndex
                );

                if (encryptedBytes != null && encryptedBytes.length > 0) {
                    decryptedBytes = encryptionService.decryptChunk(encryptedBytes, sessionId, chunkIndex);
                    log.info("[FileDownloadProcessor] PRIMARY Chunk #{} retrieved & decrypted successfully.", chunkIndex);
                }
            } catch (Exception e) {
                log.warn("[FileDownloadProcessor] PRIMARY Chunk #{} download or decryption failed! Reason: {}. Reporting tamper event to Core Engine & falling back to REPLICA...",
                        chunkIndex, e.getMessage());

                // Report missing/tampered primary chunk to Core Engine to trigger Admin Warning Flags
                reportTamperedChunkToCore(sessionId, chunkIndex);
            }
        }

        // --- STEP 2: Fallback to Replica Copy if Primary Failed ---
        if (decryptedBytes == null && replicaToken != null && replicaToken.getRefreshToken() != null && !replicaToken.getRefreshToken().isBlank()) {
            try {
                log.info("[FileDownloadProcessor] Attempting REPLICA download from [{}] for Chunk #{}",
                        replicaToken.getProviderName(), chunkIndex);

                encryptedBytes = multiCloudRouter.downloadChunkFromCloud(
                        replicaToken.getProviderName(),
                        replicaToken.getRefreshToken(),
                        sessionId,
                        chunkIndex
                );

                if (encryptedBytes != null && encryptedBytes.length > 0) {
                    decryptedBytes = encryptionService.decryptChunk(encryptedBytes, sessionId, chunkIndex);
                    log.info("[FileDownloadProcessor] REPLICA Chunk #{} recovered & decrypted successfully!", chunkIndex);
                }
            } catch (Exception e) {
                log.error("[FileDownloadProcessor] REPLICA Chunk #{} download also failed: {}", chunkIndex, e.getMessage(), e);
                throw new RuntimeException("Both Primary and Replica downloads failed for Chunk #" + chunkIndex, e);
            }
        }

        if (decryptedBytes == null) {
            throw new IllegalStateException("Primary chunk #" + chunkIndex + " failed and no valid replica token was available.");
        }

        // --- STEP 3: Detect Last Chunk & Output Completion Banner ---
        if (chunkIndex == maxChunkIndex) {
            log.info("==========================================================================================");
            log.info("🎉 [FileDownloadProcessor] ALL CHUNKS ({}/{}) DOWNLOADED & REASSEMBLED SUCCESSFULLY FOR SESSION: {}",
                    chunkIndex + 1, maxChunkIndex + 1, sessionId);
            log.info("==========================================================================================");
        }

        return decryptedBytes;
    }

    private void reportTamperedChunkToCore(String sessionId, int chunkIndex) {
        try {
            String url = coreEngineBaseUrl + "/transfers/" + sessionId + "/chunks/" + chunkIndex + "/tampered";
            restTemplate.postForEntity(url, null, Map.class);
            log.info("[FileDownloadProcessor] Reported tampered/missing primary chunk #{} for session {} to Core Engine.", chunkIndex, sessionId);
        } catch (Exception ex) {
            log.error("[FileDownloadProcessor] Could not notify Core Engine of tampered chunk #{}: {}", chunkIndex, ex.getMessage());
        }
    }
}