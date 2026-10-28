package com.cloudtransfer.stream.storage;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class  GoogleDriveOAuthStorageProvider {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchanges the volunteer's permanent refreshToken (from PostgreSQL) for a short-lived accessToken at runtime.
     */
    private String fetchAccessTokenFromRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Volunteer refresh token cannot be empty.");
        }

        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                if (accessToken != null && !accessToken.isBlank()) {
                    log.info("[GoogleDriveProvider] Successfully obtained runtime access token for volunteer node.");
                    return accessToken;
                }
            }
        } catch (Exception e) {
            log.error("[GoogleDriveProvider] Failed to exchange refresh token for access token: {}", e.getMessage(), e);
            throw new RuntimeException("Google Drive token exchange failed: " + e.getMessage(), e);
        }

        throw new IllegalStateException("Could not obtain valid access token from Google OAuth endpoint.");
    }

    /**
     * Checks if 'CloudTransfer_Vault' folder exists in contributor's Google Drive.
     * If not found, creates it and returns the folder ID.
     */
    private String getOrCreateVaultFolderId(Drive driveService) throws Exception {
        String folderName = "CloudTransfer_Vault";
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Folder doesn't exist; create it
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        log.info("[GoogleDriveProvider] Created 'CloudTransfer_Vault' folder on volunteer Drive. Folder ID: {}", folder.getId());
        return folder.getId();
    }

    public String uploadChunkToContributor(String refreshToken, String sessionId, int chunkIndex, String filename, byte[] data) {
        log.info("[GoogleDriveProvider] Uploading chunk #{} ({} bytes) for session {} to Google Drive", chunkIndex, data.length, sessionId);

        try {
            String accessToken = fetchAccessTokenFromRefreshToken(refreshToken);

            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
            Drive driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CloudTransfer-App/1.0")
                    .build();

            // Get or create dedicated 'CloudTransfer_Vault' folder ID
            String vaultFolderId = getOrCreateVaultFolderId(driveService);

            File fileMetadata = new File();
            fileMetadata.setName(sessionId + "_chunk_" + chunkIndex + "_" + filename);
            fileMetadata.setParents(Collections.singletonList(vaultFolderId));

            ByteArrayContent mediaContent = new ByteArrayContent("application/octet-stream", data);
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute();

            log.info("[GoogleDriveProvider] Successfully uploaded chunk to Google Drive in CloudTransfer_Vault. File ID: {}", uploadedFile.getId());
            return uploadedFile.getId();

        } catch (Exception e) {
            log.error("[GoogleDriveProvider] Google Drive chunk upload failed for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Google Drive upload failed: " + e.getMessage(), e);
        }
    }

    public String uploadChunk(String refreshToken, String sessionId, int chunkIndex, String filename, byte[] data) {
        return uploadChunkToContributor(refreshToken, sessionId, chunkIndex, filename, data);
    }

    public byte[] downloadChunkFromContributor(String refreshToken, String sessionId, int chunkIndex) {
        log.info("[GoogleDriveProvider] Downloading chunk #{} for session {} from Google Drive", chunkIndex, sessionId);
        try {
            String accessToken = fetchAccessTokenFromRefreshToken(refreshToken);

            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
            Drive driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CloudTransfer-App/1.0")
                    .build();

            String query = "name contains '" + sessionId + "_chunk_" + chunkIndex + "' and trashed=false";
            var fileList = driveService.files().list().setQ(query).execute();

            if (fileList.getFiles() == null || fileList.getFiles().isEmpty()) {
                log.warn("[GoogleDriveProvider] Chunk file not found in Google Drive for session {}", sessionId);
                throw new IllegalStateException("Chunk file not found in Google Drive for session " + sessionId);
            }

            String fileId = fileList.getFiles().get(0).getId();
            try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                return outputStream.toByteArray();
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GoogleDriveProvider] Google Drive chunk download failed for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Google Drive download failed: " + e.getMessage(), e);
        }
    }
}