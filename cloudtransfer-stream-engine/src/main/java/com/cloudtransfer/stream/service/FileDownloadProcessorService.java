package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.dto.ChunkDownloadInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileDownloadProcessorService {

    private final EncryptionService encryptionService;
    private final CloudStorageUploaderService cloudStorageUploaderService;

    public byte[] downloadAndReassembleFile(String sessionId, List<ChunkDownloadInfo> chunksToDownload) {
        log.info("Starting download and decryption pipeline for session: {}", sessionId);
        ByteArrayOutputStream reassembledFileStream = new ByteArrayOutputStream();

        try {
            chunksToDownload.sort((a, b) -> Integer.compare(a.getChunkIndex(), b.getChunkIndex()));

            for (ChunkDownloadInfo chunkInfo : chunksToDownload) {
                byte[] encryptedChunk = null;

                if ("GOOGLE_DRIVE".equalsIgnoreCase(chunkInfo.getProviderName())) {
                    String accessToken = cloudStorageUploaderService.refreshGoogleAccessToken(chunkInfo.getRefreshToken());
                    if (accessToken != null) {
                        encryptedChunk = cloudStorageUploaderService.downloadChunkFromGoogleDrive(accessToken, chunkInfo.getCloudFileId());
                    }
                } else if ("DROPBOX".equalsIgnoreCase(chunkInfo.getProviderName())) {
                    String accessToken = cloudStorageUploaderService.refreshDropboxAccessToken(chunkInfo.getRefreshToken());
                    if (accessToken != null) {
                        encryptedChunk = cloudStorageUploaderService.downloadChunkFromDropbox(accessToken, chunkInfo.getCloudFileId());
                    }
                }

                if (encryptedChunk == null) {
                    throw new RuntimeException("Failed to download chunk index " + chunkInfo.getChunkIndex() + " from " + chunkInfo.getProviderName());
                }

                String downloadedHash = encryptionService.calculateSha256Hash(encryptedChunk);
                if (!downloadedHash.equalsIgnoreCase(chunkInfo.getExpectedHash())) {
                    log.error("Integrity check failed for chunk {}: Hash mismatch!", chunkInfo.getChunkIndex());
                    throw new SecurityException("Chunk integrity verification failed!");
                }

                byte[] decryptedChunk = encryptionService.decryptChunk(encryptedChunk);
                reassembledFileStream.write(decryptedChunk);
                log.info("Successfully downloaded, verified, and decrypted chunk #{}", chunkInfo.getChunkIndex());
            }

            log.info("Successfully completed file reassembly for session: {}", sessionId);
            return reassembledFileStream.toByteArray();

        } catch (Exception e) {
            log.error("Error during file download and decryption: {}", e.getMessage(), e);
            return null;
        }
    }
}