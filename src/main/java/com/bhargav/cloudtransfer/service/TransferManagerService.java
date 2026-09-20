package com.bhargav.cloudtransfer.service;

import com.bhargav.cloudtransfer.entity.*;
import com.bhargav.cloudtransfer.repository.ChunkDistributionRepository;
import com.bhargav.cloudtransfer.repository.ChunkRepository;
import com.bhargav.cloudtransfer.repository.ContributorRepository;
import com.bhargav.cloudtransfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferManagerService {

    private final TransferRepository transferRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkDistributionRepository chunkDistributionRepository;
    private final ContributorRepository contributorRepository;

    private final ChunkingService chunkingService;
    private final EncryptionService encryptionService;
    private final GoogleDriveProviderService googleDriveProviderService;
    private final StorageOrchestratorService storageOrchestratorService;

    public String initiateTransfer(MultipartFile file, String senderEmail, int expiryMinutes) throws Exception {
        log.info("Initiating transfer for file: {}", file.getOriginalFilename());

        Transfer transfer = new Transfer();
        transfer.setSenderEmail(senderEmail);
        transfer.setFileName(file.getOriginalFilename());
        transfer.setTotalSizeBytes(file.getSize());
        transfer.setStatus(TransferStatus.PROCESSING);
        transfer.setCreatedAt(LocalDateTime.now());
        transfer.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        transferRepository.save(transfer);

        // 1. Fetch available contributors FIRST
        List<Contributor> activeContributors = contributorRepository.findAll().stream()
                .filter(Contributor::isActive)
                .toList();

        if (activeContributors.isEmpty()) {
            throw new RuntimeException("No active storage contributors available!");
        }

        // 2. Data Slicing (Dynamic Capacity-Weighted Chunking)
        List<byte[]> rawChunks = chunkingService.splitFileDynamically(file, activeContributors);

        SecretKey transferKey = encryptionService.generateTransferKey();

        // 3. Encrypt and Upload each chunk
        for (int i = 0; i < rawChunks.size(); i++) {
            byte[] rawChunk = rawChunks.get(i);

            // Directly match chunk to its weighted drive based on the index
            Contributor selectedContributor = activeContributors.get(i);

            EncryptedChunkPayload encryptedPayload = encryptionService.encryptChunk(rawChunk, transferKey);

            Chunk chunk = new Chunk();
            chunk.setTransfer(transfer);
            chunk.setChunkIndex(i);
            chunk.setSizeBytes((long) rawChunk.length);
            chunk.setEncryptionNonce(Base64.getEncoder().encodeToString(encryptedPayload.getNonce()));
            chunk.setStatus(ChunkStatus.PENDING);
            chunkRepository.save(chunk);

            String chunkFileName = transfer.getId().toString() + "_chunk_" + i + ".bin";
            String remoteId = googleDriveProviderService.uploadChunk(
                    encryptedPayload.getCiphertext(),
                    chunkFileName,
                    selectedContributor.getRefreshToken(),
                    selectedContributor.getFolderId()
            );

            ChunkDistribution distribution = new ChunkDistribution();
            distribution.setChunk(chunk);
            distribution.setContributor(selectedContributor);
            distribution.setRemoteReferenceId(remoteId);
            distribution.setPrimary(true);
            chunkDistributionRepository.save(distribution);

            // Commit the storage usage immediately so the database reflects the consumed space
            long chunkSizeBytes = (long) rawChunk.length;
            storageOrchestratorService.commitStorageUsage(selectedContributor, chunkSizeBytes);

            chunk.setStatus(ChunkStatus.UPLOADED);
            chunkRepository.save(chunk);
        }

        transfer.setStatus(TransferStatus.AVAILABLE);
        transferRepository.save(transfer);
        log.info("Transfer {} successfully processed and uploaded.", transfer.getId());

        return "Transfer ID: " + transfer.getId() + "\nMaster Key (Base64): " + Base64.getEncoder().encodeToString(transferKey.getEncoded());
    }

    public byte[] downloadTransfer(UUID transferId, String base64MasterKey) throws Exception {
        log.info("Initiating download for transfer: {}", transferId);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found or expired"));
        SecretKey transferKey = new SecretKeySpec(Base64.getDecoder().decode(base64MasterKey), "ChaCha20");

        List<Chunk> chunks = chunkRepository.findAllByTransferOrderByChunkIndexAsc(transfer);
        ByteArrayOutputStream mergedFileStream = new ByteArrayOutputStream();

        for (Chunk chunk : chunks) {
            ChunkDistribution distribution = chunkDistributionRepository.findFirstByChunk(chunk);
            Contributor contributor = distribution.getContributor();

            byte[] encryptedChunk = googleDriveProviderService.downloadChunk(
                    distribution.getRemoteReferenceId(),
                    contributor.getRefreshToken()
            );

            byte[] nonce = Base64.getDecoder().decode(chunk.getEncryptionNonce());
            byte[] decryptedChunk = encryptionService.decryptChunk(encryptedChunk, transferKey, nonce);
            mergedFileStream.write(decryptedChunk);
        }

        log.info("Successfully downloaded, decrypted, and merged transfer {}", transferId);
        return mergedFileStream.toByteArray();
    }

    public Transfer getTransferDetails(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));
    }
}