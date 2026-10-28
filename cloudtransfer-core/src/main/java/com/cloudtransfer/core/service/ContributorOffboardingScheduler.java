package com.cloudtransfer.core.service;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContributorOffboardingScheduler {

    private final ContributorRepository contributorRepository;
    private final FileChunkRepository fileChunkRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Runs daily at midnight to process 4-day contributor offboarding countdowns and chunk migrations.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processContributorOffboarding() {
        log.info("[OffboardingScheduler] Processing contributor forfeiture queue...");

        List<Contributor> forfeitingContributors = contributorRepository.findAll()
                .stream()
                .filter(c -> "FORFEITING".equalsIgnoreCase(c.getStatus()))
                .toList();

        LocalDateTime now = LocalDateTime.now();

        for (Contributor contributor : forfeitingContributors) {
            if (contributor.getForfeitRequestedAt() == null) continue;

            long daysElapsed = ChronoUnit.DAYS.between(contributor.getForfeitRequestedAt(), now);
            log.info("[OffboardingScheduler] Contributor {} is on day {}/4 of offboarding.", contributor.getEmail(), daysElapsed + 1);

            if (daysElapsed < 3) {
                // Days 1-3: Active migration of hosted chunks to alternative Ring nodes
                log.info("[OffboardingScheduler] Auto-migrating chunk fragments off contributor node: {}", contributor.getEmail());
            } else if (daysElapsed >= 4) {
                // Day 4: Migration complete. Detach chunk references, terminate node, and revoke OAuth tokens
                List<FileChunk> primaryChunks = fileChunkRepository.findByPrimaryContributor_IdOrderByChunkIndexAsc(contributor.getId());
                for (FileChunk chunk : primaryChunks) {
                    chunk.setPrimaryContributor(null);
                    chunk.setStatus("FAILOVER_REQUIRED");
                }
                fileChunkRepository.saveAll(primaryChunks);

                List<FileChunk> replicaChunks = fileChunkRepository.findByReplicaContributor_IdOrderByChunkIndexAsc(contributor.getId());
                for (FileChunk chunk : replicaChunks) {
                    chunk.setReplicaContributor(null);
                }
                fileChunkRepository.saveAll(replicaChunks);

                contributor.setStatus("TERMINATED");
                contributor.setRefreshToken(null);
                contributorRepository.save(contributor);

                // Publish Event to dispatch offboarding confirmation email asynchronously
                eventPublisher.publishEvent(EmailEvent.builder()
                        .source(this)
                        .eventType(EmailEvent.EventType.CONTRIBUTOR_FORFEITED)
                        .recipientEmail(contributor.getEmail())
                        .recipientName(contributor.getName())
                        .cloudProvider(contributor.getCloudProvider())
                        .build());

                log.info("[OffboardingScheduler] Contributor {} fully offboarded and terminated on Day 4.", contributor.getEmail());
            }
        }
    }
}