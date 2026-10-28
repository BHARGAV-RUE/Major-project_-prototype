package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.dto.CloudProviderTokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileChunkProcessorService {

    private final CloudStorageUploaderService cloudStorageUploaderService;
    private final EncryptionService encryptionService;
    private final HashService hashService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cloudtransfer.core.url:http://localhost:8080/api/v1}")
    private String coreEngineBaseUrl;

    private final Map<String, List<CloudProviderTokenDTO>> sessionTokenRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> sessionHashBuffer = new ConcurrentHashMap<>();

    public void registerSessionTokens(String sessionId, int totalChunks, List<CloudProviderTokenDTO> tokens) {
        if (tokens != null) {
            sessionTokenRegistry.put(sessionId, tokens);
        }
        sessionHashBuffer.computeIfAbsent(sessionId, k -> new ArrayList<>());
        log.info("[FileChunkProcessor] Session {} pre-registered with {} tokens.", sessionId, tokens != null ? tokens.size() : 0);
    }

    public List<Map<String, Object>> getSessionHashes(String sessionId) {
        return sessionHashBuffer.getOrDefault(sessionId, new ArrayList<>());
    }

    public void processAndUploadFile(String fileId, String filename, String filePath, List<CloudProviderTokenDTO> tokens) {
        log.info("[FileChunkProcessor] Processing background upload for fileId: {}, filename: {}, path: {}",
                fileId, filename, filePath);

        try {
            if (tokens == null || tokens.isEmpty()) {
                tokens = fetchTokensFromCoreEngine(fileId);
            }

            File fileOnDisk = new File(filePath);
            byte[] fileBytes;
            if (fileOnDisk.exists()) {
                fileBytes = Files.readAllBytes(fileOnDisk.toPath());
            } else {
                log.warn("[FileChunkProcessor] File path not found: {}. Using empty byte payload.", filePath);
                fileBytes = new byte[0];
            }

            processAndUploadChunkBytes(fileId, 0, filename, fileBytes, tokens);

        } catch (Exception e) {
            log.error("[FileChunkProcessor] Background upload processing failed for fileId {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Background file processing failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> processAndUploadMultipleChunks(
            String sessionId,
            Integer chunkIndex,
            MultipartFile[] files,
            List<CloudProviderTokenDTO> tokenList) throws Exception {

        log.info("[FileChunkProcessor] Processing HTTP upload for session: {}, chunkIndex: {}", sessionId, chunkIndex);

        if (tokenList == null || tokenList.isEmpty()) {
            tokenList = sessionTokenRegistry.get(sessionId);
        }

        if (tokenList == null || tokenList.isEmpty()) {
            tokenList = fetchTokensFromCoreEngine(sessionId);
        }

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No file payload provided for upload.");
        }

        MultipartFile file = files[0];
        byte[] rawBytes = file.getBytes();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "chunk_" + chunkIndex;

        return processAndUploadChunkBytes(sessionId, chunkIndex, filename, rawBytes, tokenList);
    }

    public Map<String, Object> processAndUploadChunkBytes(
            String sessionId,
            int chunkIndex,
            String filename,
            byte[] rawBytes,
            List<CloudProviderTokenDTO> tokenList) throws Exception {

        if (tokenList == null || tokenList.isEmpty()) {
            tokenList = sessionTokenRegistry.get(sessionId);
        }

        if (tokenList == null || tokenList.isEmpty()) {
            tokenList = fetchTokensFromCoreEngine(sessionId);
        }

        if (tokenList == null || tokenList.isEmpty()) {
            throw new IllegalStateException("No volunteer tokens available for session " + sessionId);
        }

        // Calculate maximum chunk index to detect final chunk completion
        int maxChunkIndex = tokenList.stream()
                .mapToInt(CloudProviderTokenDTO::getChunkIndex)
                .max()
                .orElse(0);

        // Extract Primary and Replica tokens for this chunk index
        CloudProviderTokenDTO primaryToken = tokenList.stream()
                .filter(t -> t.getChunkIndex() == chunkIndex && !t.isReplica())
                .findFirst()
                .orElse(tokenList.get(0));

        CloudProviderTokenDTO replicaToken = tokenList.stream()
                .filter(t -> t.getChunkIndex() == chunkIndex && t.isReplica())
                .findFirst()
                .orElse(null);

        // 1. Encrypt raw chunk bytes
        byte[] encryptedBytes = encryptionService.encryptChunk(rawBytes, sessionId, chunkIndex);

        // 2. Calculate SHA-256 hash
        String sha256Hash = hashService.calculateSha256(encryptedBytes);

        // 3. Upload Primary Copy to volunteer cloud
        String primaryCloudFileId = cloudStorageUploaderService.uploadPrimaryChunk(
                primaryToken.getProviderName(),
                primaryToken.getRefreshToken(),
                sessionId,
                chunkIndex,
                filename,
                encryptedBytes
        );

        // 4. Upload Replica Copy to volunteer cloud
        String replicaCloudFileId = null;
        if (replicaToken != null) {
            replicaCloudFileId = cloudStorageUploaderService.uploadReplicaChunk(
                    replicaToken.getProviderName(),
                    replicaToken.getRefreshToken(),
                    sessionId,
                    chunkIndex,
                    filename,
                    encryptedBytes
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("chunkIndex", chunkIndex);
        response.put("status", "SUCCESS");
        response.put("primaryCloudFileId", primaryCloudFileId);
        response.put("replicaCloudFileId", replicaCloudFileId);
        response.put("sha256Hash", sha256Hash);

        // Buffer the hash data in memory for synchronous retrieval by Core Engine
        sessionHashBuffer.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(response);

        // --- STEP 5: Detect Last Chunk & Output Completion Banner ---
        if (chunkIndex == maxChunkIndex) {
            log.info("==========================================================================================");
            log.info("✅ [FileChunkProcessor] ALL CHUNKS ({}/{}) UPLOADED & ANCHORED SUCCESSFULLY FOR SESSION: {}",
                    chunkIndex + 1, maxChunkIndex + 1, sessionId);
            log.info("==========================================================================================");
        }

        return response;
    }

    private List<CloudProviderTokenDTO> fetchTokensFromCoreEngine(String sessionId) {
        try {
            String coreUrl = coreEngineBaseUrl + "/transfers/" + sessionId + "/chunk-assignments";
            ResponseEntity<List<CloudProviderTokenDTO>> response = restTemplate.exchange(
                    coreUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CloudProviderTokenDTO>>() {}
            );
            List<CloudProviderTokenDTO> fetchedTokens = response.getBody();
            if (fetchedTokens != null) {
                sessionTokenRegistry.put(sessionId, fetchedTokens);
            }
            return fetchedTokens;
        } catch (Exception e) {
            log.error("[FileChunkProcessor] Could not fetch tokens from Core Engine for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }
}