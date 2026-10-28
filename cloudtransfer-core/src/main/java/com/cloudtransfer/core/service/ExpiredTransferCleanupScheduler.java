package com.cloudtransfer.core.service;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpiredTransferCleanupScheduler {

    private final TransferSessionRepository transferSessionRepository;
    private final FileChunkRepository fileChunkRepository;
    private final ContributorRepository contributorRepository;

    /**
     * Runs automatically every 15 minutes to purge expired file transfers,
     * reclaim contributor storage space, and mark sessions as EXPIRED.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes (900,000 ms)
    @Transactional
    public void purgeExpiredTransfers() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Fetch sessions that passed their expiry timestamp and are not yet marked EXPIRED
        List<TransferSession> expiredSessions = transferSessionRepository.findAll().stream()
                .filter(s -> s.getExpiryTimestamp() != null && now.isAfter(s.getExpiryTimestamp()))
                .filter(s -> !"EXPIRED".equalsIgnoreCase(s.getStatus()))
                .toList();

        if (expiredSessions.isEmpty()) {
            return;
        }

        log.info("[ExpiredCleanup] Found {} expired transfer session(s) to process.", expiredSessions.size());

        for (TransferSession session : expiredSessions) {
            log.info("[ExpiredCleanup] Purging expired session ID: {} ({})", session.getId(), session.getOriginalFilename());

            List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(session.getId());

            for (FileChunk chunk : chunks) {
                // 2. Reclaim storage on Primary Contributor Node
                if (chunk.getPrimaryContributor() != null) {
                    Contributor primary = chunk.getPrimaryContributor();
                    long currentUsed = primary.getUsedStorageBytes() != null ? primary.getUsedStorageBytes() : 0L;
                    long newUsed = Math.max(0L, currentUsed - chunk.getChunkSizeBytes());
                    primary.setUsedStorageBytes(newUsed);
                    contributorRepository.save(primary);
                }

                // 3. Reclaim storage on Replica Contributor Node
                if (chunk.getReplicaContributor() != null) {
                    Contributor replica = chunk.getReplicaContributor();
                    long currentUsed = replica.getUsedStorageBytes() != null ? replica.getUsedStorageBytes() : 0L;
                    long newUsed = Math.max(0L, currentUsed - chunk.getChunkSizeBytes());
                    replica.setUsedStorageBytes(newUsed);
                    contributorRepository.save(replica);
                }

                // 4. Mark chunk status as EXPIRED
                chunk.setStatus("EXPIRED");
            }

            fileChunkRepository.saveAll(chunks);

            // 5. Update overall session status
            session.setStatus("EXPIRED");
            transferSessionRepository.save(session);

            log.info("[ExpiredCleanup] Expired session ID {} processed and contributor capacity reclaimed.", session.getId());
        }
    }
}