package com.bhargav.cloudtransfer.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.services.drive.model.About;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

@Slf4j
@Service
public class GoogleDriveProviderService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    /**
     * Dynamically builds a Google Drive client using a contributor's OAuth refresh token.
     */
    private Drive getDriveClient(String refreshToken) throws Exception {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token is missing or null. Please ensure the contributor is properly registered via OAuth.");
        }

        // 1. Build OAuth 2.0 User Credentials using the stored refresh token
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        // 2. Return the authenticated Google Drive service
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudTransferPlatform")
                .build();
    }

    /**
     * Uploads an encrypted chunk to a specific contributor's Google Drive.
     */
    public String uploadChunk(byte[] chunkData, String fileName, String refreshToken, String targetFolderId) throws Exception {
        log.info("Authenticating contributor and uploading chunk {}...", fileName);

        Drive driveService = getDriveClient(refreshToken);

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(targetFolderId));

        InputStreamContent mediaContent = new InputStreamContent(
                "application/octet-stream",
                new ByteArrayInputStream(chunkData)
        );

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        log.info("Successfully uploaded to contributor's Google Drive. Remote ID: {}", uploadedFile.getId());
        return uploadedFile.getId();
    }

    /**
     * Downloads an encrypted chunk from a contributor's Google Drive.
     */
    public byte[] downloadChunk(String remoteFileId, String refreshToken) throws Exception {
        log.info("Authenticating contributor and downloading chunk {}...", remoteFileId);

        Drive driveService = getDriveClient(refreshToken);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(remoteFileId).executeMediaAndDownloadTo(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Deletes the chunk from a contributor's Google Drive upon transfer expiration.
     */
    public void deleteChunk(String remoteFileId, String refreshToken) {
        try {
            Drive driveService = getDriveClient(refreshToken);
            driveService.files().delete(remoteFileId).execute();
            log.info("Successfully deleted chunk {} to reclaim storage.", remoteFileId);
        } catch (Exception e) {
            log.error("Failed to delete chunk {}: {}", remoteFileId, e.getMessage());
        }
    }
    public long checkAvailableStorage(String refreshToken) throws Exception {
        Drive driveService = getDriveClient(refreshToken);
        About about = driveService.about().get().setFields("storageQuota").execute();

        Long limit = about.getStorageQuota().getLimit();
        Long usage = about.getStorageQuota().getUsage();

        // If limit is null, the user has unlimited storage (e.g., enterprise workspace)
        if (limit == null) return Long.MAX_VALUE;

        return limit - usage;
    }

    public String getOrCreateAppFolder(String refreshToken) throws Exception {
        Drive driveService = getDriveClient(refreshToken);
        String folderName = "CloudTransferChunks";

        String query = "mimeType = 'application/vnd.google-apps.folder' and name = '" + folderName + "' and trashed = false";
        FileList result = driveService.files().list().setQ(query).setSpaces("drive").execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(fileMetadata).setFields("id").execute();
        return folder.getId();
    }
}