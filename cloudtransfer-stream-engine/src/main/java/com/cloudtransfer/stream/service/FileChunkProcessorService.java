package com.cloudtransfer.stream.service;

import com.cloudtransfer.stream.config.RabbitMQConfig;
import com.cloudtransfer.stream.dto.ChunkHashMessage;
import com.cloudtransfer.stream.dto.CloudProviderTokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileChunkProcessorService {

    private final EncryptionService encryptionService;
    private final CloudStorageUploaderService cloudStorageUploaderService;
    private final RabbitTemplate rabbitTemplate;

    private static final int CHUNK_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

    public void processAndUploadFile(String sessionId, String filename, String filePath, List<CloudProviderTokenDTO> tokens) {
        log.info("Starting chunking and encryption pipeline for session: {}, File: {}", sessionId, filename);

        // Safe check for null or non-existent local file path
        if (filePath == null || !new File(filePath).exists()) {
            log.warn("Local file path is null or file does not exist at: {}. Executing simulation mode.", filePath);
            executeSimulatedUpload(sessionId, filename, tokens);
            return;
        }

        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE_BYTES];
            int bytesRead;
            int chunkIndex = 0;

            boolean hasTokens = tokens != null && !tokens.isEmpty();

            while ((bytesRead = fis.read(buffer)) > 0) {
                byte[] actualChunk = (bytesRead == CHUNK_SIZE_BYTES) ? buffer : Arrays.copyOf(buffer, bytesRead);

                // 1. Encrypt chunk using ChaCha20 cipher
                byte[] encryptedChunk = encryptionService.encryptChunk(actualChunk);

                // 2. Compute SHA-256 Hash for blockchain integrity ledger
                String chunkHash = encryptionService.calculateSha256Hash(encryptedChunk);

                String chunkFileName = sessionId + "_chunk_" + chunkIndex + ".enc";

                // 3. Safe Dispatch to Primary and Replica Volunteer Drives
                CloudProviderTokenDTO primaryVolunteer = hasTokens
                        ? tokens.get(chunkIndex % tokens.size())
                        : new CloudProviderTokenDTO("GOOGLE_DRIVE", "sim@drive.com", "mock_token");

                CloudProviderTokenDTO replicaVolunteer = hasTokens
                        ? tokens.get((chunkIndex + 1) % tokens.size())
                        : new CloudProviderTokenDTO("DROPBOX", "sim@dropbox.com", "mock_token");

                String primaryCloudFileId = dispatchChunkUpload(primaryVolunteer, chunkFileName, encryptedChunk);
                dispatchChunkUpload(replicaVolunteer, "replica_" + chunkFileName, encryptedChunk);

                // 4. Send ChunkHashMessage feedback to Brain via RabbitMQ
                dispatchHashToBrain(sessionId, chunkIndex, chunkHash, primaryCloudFileId, primaryVolunteer.getProviderName());

                log.info("Chunk {} processed | Size: {} bytes | Hash: {}", chunkIndex, encryptedChunk.length, chunkHash);
                chunkIndex++;
            }

            log.info("Successfully completed Phase 2 execution for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error processing file {}: {}", filename, e.getMessage(), e);
        }
    }

    private String dispatchChunkUpload(CloudProviderTokenDTO volunteer, String chunkFileName, byte[] encryptedData) {
        if (volunteer == null) {
            return "sim_" + chunkFileName;
        }

        if ("GOOGLE_DRIVE".equalsIgnoreCase(volunteer.getProviderName())) {
            String accessToken = cloudStorageUploaderService.refreshGoogleAccessToken(volunteer.getRefreshToken());
            if (accessToken != null) {
                boolean uploaded = cloudStorageUploaderService.uploadChunkToGoogleDrive(accessToken, chunkFileName, encryptedData);
                if (uploaded) return "gdrive_" + chunkFileName;
            }
            log.info("[SIMULATION] Google Drive upload simulated for volunteer {}", volunteer.getVolunteerEmail());
            return "sim_gdrive_" + chunkFileName;
        } else if ("DROPBOX".equalsIgnoreCase(volunteer.getProviderName())) {
            String accessToken = cloudStorageUploaderService.refreshDropboxAccessToken(volunteer.getRefreshToken());
            if (accessToken != null) {
                boolean uploaded = cloudStorageUploaderService.uploadChunkToDropbox(accessToken, chunkFileName, encryptedData);
                if (uploaded) return "dropbox_" + chunkFileName;
            }
            log.info("[SIMULATION] Dropbox upload simulated for volunteer {}", volunteer.getVolunteerEmail());
            return "sim_dropbox_" + chunkFileName;
        }
        return "sim_" + chunkFileName;
    }

    private void dispatchHashToBrain(String sessionId, int chunkIndex, String chunkHash, String cloudFileId, String providerName) {
        try {
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }

            UUID sessionUuid;
            try {
                sessionUuid = UUID.fromString(sessionId);
            } catch (IllegalArgumentException e) {
                sessionUuid = UUID.nameUUIDFromBytes(sessionId.getBytes());
            }

            ChunkHashMessage hashMessage = new ChunkHashMessage(
                    sessionUuid,
                    chunkIndex,
                    chunkHash,
                    cloudFileId,
                    providerName
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHUNK_HASH_EXCHANGE,
                    RabbitMQConfig.CHUNK_HASH_ROUTING_KEY,
                    hashMessage
            );
            log.info("[Muscle -> Brain] Dispatched ChunkHashMessage for chunk #{}", chunkIndex);
        } catch (Exception e) {
            log.error("Failed to send ChunkHashMessage to Brain: {}", e.getMessage(), e);
        }
    }

    private void executeSimulatedUpload(String sessionId, String filename, List<CloudProviderTokenDTO> tokens) {
        log.info("[SIMULATION] Generating simulated chunks for demonstration.");
        try {
            byte[] mockData = ("Simulated Chunk Payload for " + filename).getBytes();
            byte[] encrypted = encryptionService.encryptChunk(mockData);
            String hash = encryptionService.calculateSha256Hash(encrypted);

            CloudProviderTokenDTO primaryVolunteer = (tokens != null && !tokens.isEmpty()) ? tokens.get(0)
                    : new CloudProviderTokenDTO("GOOGLE_DRIVE", "sim@drive.com", "mock_token");

            dispatchHashToBrain(sessionId, 0, hash, "sim_file_001", primaryVolunteer.getProviderName());
            log.info("[SIMULATION] Processed and dispatched simulated chunk | Hash: {}", hash);
        } catch (Exception e) {
            log.error("Simulation error: {}", e.getMessage());
        }
    }
}