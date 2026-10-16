package com.cloudtransfer.stream.storage;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class DropboxStorageProvider implements CloudStorageProvider {

    @Value("${dropbox.app.key:dummy_key}")
    private String appKey;

    @Value("${dropbox.app.secret:dummy_secret}")
    private String appSecret;

    /**
     * Creates a dynamic Dropbox client using the contributor's stored refresh token.
     */
    private DbxClientV2 buildDropboxClient(String refreshToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("CloudTransfer-Engine").build();
        return new DbxClientV2(config, refreshToken);
    }

    @Override
    public String getProviderName() {
        return "DROPBOX";
    }

    @Override
    public String uploadChunk(String sessionId, int chunkIndex, byte[] encryptedData) throws Exception {
        // Fallback for generic routing if no user token passed directly
        throw new UnsupportedOperationException("Use uploadChunkToContributor with refresh token");
    }

    /**
     * Uploads an encrypted chunk to a contributor's personal Dropbox space.
     */
    public String uploadChunkToContributor(String refreshToken, String sessionId, int chunkIndex, byte[] encryptedData) throws Exception {
        DbxClientV2 client = buildDropboxClient(refreshToken);

        String path = "/ct_chunk_" + sessionId + "_" + chunkIndex + ".bin";

        try (InputStream in = new ByteArrayInputStream(encryptedData)) {
            FileMetadata metadata = client.files().uploadBuilder(path)
                    .uploadAndFinish(in);

            // Returns the unique Dropbox path or ID for retrieval
            return metadata.getPathLower();
        }
    }

    @Override
    public byte[] downloadChunk(String cloudFileId) throws Exception {
        // Fallback interface requirement
        throw new UnsupportedOperationException("Use downloadChunkFromContributor with refresh token");
    }

    /**
     * Downloads an encrypted chunk from a contributor's Dropbox account.
     */
    public byte[] downloadChunkFromContributor(String refreshToken, String cloudFileId) throws Exception {
        DbxClientV2 client = buildDropboxClient(refreshToken);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            client.files().downloadBuilder(cloudFileId).download(out);
            return out.toByteArray();
        }
    }

    /**
     * Queries available storage space on the contributor's Dropbox account.
     */
    public long getAvailableStorageBytes(String refreshToken) throws Exception {
        DbxClientV2 client = buildDropboxClient(refreshToken);

        var spaceUsage = client.users().getSpaceUsage();
        long used = spaceUsage.getUsed();
        long allocated = spaceUsage.getAllocation().getIndividualValue().getAllocated();

        long freeBytes = allocated - used;

        // Reserve 1 GB safety buffer
        long usableBytes = freeBytes - (1024L * 1024L * 1024L);
        return Math.max(0, usableBytes);
    }
}