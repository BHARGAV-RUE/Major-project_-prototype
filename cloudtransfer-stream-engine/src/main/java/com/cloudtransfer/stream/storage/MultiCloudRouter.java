package com.cloudtransfer.stream.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MultiCloudRouter {

    private final GoogleDriveOAuthStorageProvider googleDriveOAuthStorageProvider;
    private final DropboxStorageProvider dropboxStorageProvider;

    /**
     * Routes encrypted chunk upload using provider name and refresh token supplied by Distribution Engine.
     */
    public String routeChunkToCloud(String providerType, String refreshToken, String sessionId,
                                    Integer chunkIndex, String filename, byte[] encryptedBytes) {

        log.info("[MultiCloudRouter] Routing Upload -> Session: {} | Chunk #{} | Target Provider: [{}]",
                sessionId, chunkIndex, providerType);

        if ("GOOGLE_DRIVE".equalsIgnoreCase(providerType)) {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Distribution Engine assigned GOOGLE_DRIVE but refresh token is missing.");
            }
            return googleDriveOAuthStorageProvider.uploadChunkToContributor(refreshToken, sessionId, chunkIndex, filename, encryptedBytes);

        } else if ("DROPBOX".equalsIgnoreCase(providerType)) {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Distribution Engine assigned DROPBOX but refresh token is missing.");
            }
            return dropboxStorageProvider.uploadChunkToContributor(refreshToken, sessionId, chunkIndex, filename, encryptedBytes);

        } else {
            throw new IllegalArgumentException("Unsupported or unknown upload provider type: " + providerType);
        }
    }

    /**
     * Fallback routing overload for single-parameter upload calls.
     */
    public String routeChunkToCloud(String sessionId, Integer chunkIndex, byte[] encryptedBytes) {
        log.warn("[MultiCloudRouter] Defaulting upload to Dropbox for session: {}", sessionId);
        return dropboxStorageProvider.uploadChunkToContributor(null, sessionId, chunkIndex, "chunk_" + chunkIndex, encryptedBytes);
    }

    /**
     * Routes encrypted chunk download from either Google Drive or Dropbox using provider name and refresh token.
     */
    public byte[] downloadChunkFromCloud(String providerType, String refreshToken, String sessionId, Integer chunkIndex) {
        log.info("[MultiCloudRouter] Routing Download -> Session: {} | Chunk #{} | Provider: [{}]",
                sessionId, chunkIndex, providerType);

        if ("GOOGLE_DRIVE".equalsIgnoreCase(providerType)) {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Cannot download from GOOGLE_DRIVE: Refresh token is missing.");
            }
            return googleDriveOAuthStorageProvider.downloadChunkFromContributor(refreshToken, sessionId, chunkIndex);

        } else if ("DROPBOX".equalsIgnoreCase(providerType)) {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Cannot download from DROPBOX: Refresh token is missing.");
            }
            return dropboxStorageProvider.downloadChunkFromContributor(refreshToken, sessionId, chunkIndex);

        } else {
            throw new IllegalArgumentException("Unsupported or unknown download provider type: " + providerType);
        }
    }

    /**
     * Fallback routing overload for single-parameter download calls.
     */
    public byte[] downloadChunkFromCloud(String sessionId, Integer chunkIndex) {
        log.warn("[MultiCloudRouter] Defaulting download to Dropbox for session: {}", sessionId);
        return dropboxStorageProvider.downloadChunkFromContributor(null, sessionId, chunkIndex);
    }
}