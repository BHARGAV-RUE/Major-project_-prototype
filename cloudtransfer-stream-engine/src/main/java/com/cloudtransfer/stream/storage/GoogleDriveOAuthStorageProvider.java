package com.cloudtransfer.stream.storage;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class GoogleDriveOAuthStorageProvider {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    /**
     * Builds a temporary Google Drive instance dynamically for a specific contributor using their stored refresh token.
     */
    private Drive buildDriveClient(String refreshToken) {
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("CloudTransfer-Network").build();
    }

    /**
     * Uploads an encrypted chunk directly to a contributor's personal Google Drive.
     */
    public String uploadChunkToContributor(String refreshToken, String sessionId, int chunkIndex, byte[] encryptedData) throws Exception {
        Drive drive = buildDriveClient(refreshToken);

        File fileMetadata = new File();
        fileMetadata.setName("ct_chunk_" + sessionId + "_" + chunkIndex + ".bin");

        ByteArrayContent mediaContent = new ByteArrayContent("application/octet-stream", encryptedData);

        File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        return uploadedFile.getId();
    }

    /**
     * Queries the contributor's live Google Drive storage quota.
     */
    public long getAvailableStorageBytes(String refreshToken) throws Exception {
        Drive drive = buildDriveClient(refreshToken);
        About about = drive.about().get().setFields("storageQuota").execute();

        About.StorageQuota quota = about.getStorageQuota();
        long totalLimit = quota.getLimit();
        long totalUsed = quota.getUsage();

        long freeBytes = totalLimit - totalUsed;

        // Subtract 1 GB safety buffer
        long usableBytes = freeBytes - (1024L * 1024L * 1024L);
        return Math.max(0, usableBytes);
    }

    /**
     * Downloads a chunk from the contributor's drive during file assembly or migration.
     */
    public byte[] downloadChunkFromContributor(String refreshToken, String cloudFileId) throws Exception {
        Drive drive = buildDriveClient(refreshToken);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        drive.files().get(cloudFileId).executeMediaAndDownloadTo(outputStream);
        return outputStream.toByteArray();
    }
}