package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.storage.MultiCloudRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudStorageUploaderService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MultiCloudRouter multiCloudRouter;

    private final Map<String, byte[]> primaryChunkCache = new ConcurrentHashMap<>();
    private final Map<String, byte[]> replicaChunkCache = new ConcurrentHashMap<>();

    @Value("${google.oauth.client-id:dummy}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:dummy}")
    private String googleClientSecret;

    @Value("${dropbox.oauth.client-id:dummy}")
    private String dropboxClientId;

    @Value("${dropbox.oauth.client-secret:dummy}")
    private String dropboxClientSecret;

    public String uploadPrimaryChunk(String sessionId, Integer chunkIndex, String filename, byte[] encryptedBytes) {
        String cacheKey = sessionId + "_" + chunkIndex;
        primaryChunkCache.put(cacheKey, encryptedBytes);

        String primaryName = "ct_primary_chunk_" + sessionId + "_" + chunkIndex + "_" + filename;
        log.info("[CloudStorageUploader] Storing Primary Copy: {}", primaryName);

        return multiCloudRouter.routeChunkToCloud(sessionId, chunkIndex, encryptedBytes);
    }

    public String uploadPrimaryChunk(String providerType, String refreshToken, String sessionId, Integer chunkIndex, String filename, byte[] encryptedBytes) {
        String cacheKey = sessionId + "_" + chunkIndex;
        primaryChunkCache.put(cacheKey, encryptedBytes);

        String primaryName = "ct_primary_chunk_" + sessionId + "_" + chunkIndex + "_" + filename;
        log.info("[CloudStorageUploader] Storing Primary Copy ({}) to Provider: {}", primaryName, providerType);

        try {
            return multiCloudRouter.routeChunkToCloud(providerType, refreshToken, sessionId, chunkIndex, primaryName, encryptedBytes);
        } catch (Exception e) {
            log.error("[CloudStorageUploader] Primary upload failed for chunk {}: {}", chunkIndex, e.getMessage(), e);
            throw new RuntimeException("Real cloud upload failed for chunk " + chunkIndex + ": " + e.getMessage(), e);
        }
    }

    public String uploadReplicaChunk(String sessionId, Integer chunkIndex, String filename, byte[] encryptedBytes) {
        String cacheKey = sessionId + "_" + chunkIndex;
        replicaChunkCache.put(cacheKey, encryptedBytes);

        String replicaName = "ct_replica_chunk_" + sessionId + "_" + chunkIndex + "_" + filename;
        log.info("[CloudStorageUploader] Storing Replica Copy: {}", replicaName);

        return multiCloudRouter.routeChunkToCloud(sessionId, chunkIndex, encryptedBytes);
    }

    public String uploadReplicaChunk(String providerType, String refreshToken, String sessionId, Integer chunkIndex, String filename, byte[] encryptedBytes) {
        String cacheKey = sessionId + "_" + chunkIndex;
        replicaChunkCache.put(cacheKey, encryptedBytes);

        String replicaName = "ct_replica_chunk_" + sessionId + "_" + chunkIndex + "_" + filename;
        log.info("[CloudStorageUploader] Storing Replica Copy ({}) to Provider: {}", replicaName, providerType);

        try {
            return multiCloudRouter.routeChunkToCloud(providerType, refreshToken, sessionId, chunkIndex, replicaName, encryptedBytes);
        } catch (Exception e) {
            log.error("[CloudStorageUploader] Replica upload failed for chunk {}: {}", chunkIndex, e.getMessage(), e);
            throw new RuntimeException("Real cloud replica upload failed for chunk " + chunkIndex + ": " + e.getMessage(), e);
        }
    }

    public String uploadToCloud(String sessionId, Integer chunkIndex, byte[] encryptedBytes) {
        return uploadPrimaryChunk(sessionId, chunkIndex, "chunk", encryptedBytes);
    }

    public byte[] downloadFromCloud(String sessionId, Integer chunkIndex) {
        String cacheKey = sessionId + "_" + chunkIndex;
        if (primaryChunkCache.containsKey(cacheKey)) {
            return primaryChunkCache.get(cacheKey);
        } else if (replicaChunkCache.containsKey(cacheKey)) {
            log.warn("[CloudStorageUploader] Primary unavailable. Falling back to REPLICA chunk for session {}", sessionId);
            return replicaChunkCache.get(cacheKey);
        }
        log.warn("[CloudStorageUploader] Chunk {} for session {} not found in cache.", chunkIndex, sessionId);
        return new byte[0];
    }

    public String refreshGoogleAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.error("Google Drive refresh token is missing or empty.");
            return null;
        }

        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Google Drive token: {}", e.getMessage(), e);
        }
        return null;
    }

    public String refreshDropboxAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.error("Dropbox refresh token is missing or empty.");
            return null;
        }

        String url = "https://api.dropbox.com/oauth2/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", dropboxClientId);
        body.add("client_secret", dropboxClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Dropbox token: {}", e.getMessage(), e);
        }
        return null;
    }

    public byte[] downloadChunkFromGoogleDrive(String accessToken, String cloudFileId) {
        if (accessToken == null || cloudFileId == null) {
            log.error("Cannot download from Google Drive: accessToken or cloudFileId is null.");
            return null;
        }

        String downloadUrl = "https://www.googleapis.com/drive/v3/files/" + cloudFileId + "?alt=media";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(downloadUrl, HttpMethod.GET, request, byte[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Google Drive chunk download failed for fileId {}: {}", cloudFileId, e.getMessage(), e);
        }
        return null;
    }

    public byte[] downloadChunkFromDropbox(String accessToken, String cloudFileId) {
        if (accessToken == null || cloudFileId == null) {
            log.error("Cannot download from Dropbox: accessToken or cloudFileId is null.");
            return null;
        }

        String downloadUrl = "https://content.dropboxapi.com/2/files/download";
        String pathArg = cloudFileId.startsWith("/") ? cloudFileId : "/" + cloudFileId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Dropbox-API-Arg", "{\"path\": \"" + pathArg + "\"}");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(downloadUrl, HttpMethod.POST, request, byte[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Dropbox chunk download failed for path {}: {}", cloudFileId, e.getMessage(), e);
        }
        return null;
    }
}