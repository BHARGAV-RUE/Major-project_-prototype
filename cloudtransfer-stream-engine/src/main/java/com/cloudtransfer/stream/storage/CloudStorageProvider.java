package com.cloudtransfer.stream.storage;

public interface CloudStorageProvider {
    /**
     * Identifies which cloud provider this is (e.g., "GOOGLE_DRIVE", "DROPBOX").
     */
    String getProviderName();

    /**
     * Uploads the encrypted chunk byte array to the cloud provider.
     * @return The unique Cloud File ID or URL to retrieve it later.
     */
    String uploadChunk(String sessionId, int chunkIndex, byte[] encryptedData) throws Exception;

    /**
     * Downloads the encrypted chunk from the cloud provider using the File ID.
     */
    byte[] downloadChunk(String cloudFileId) throws Exception;
}