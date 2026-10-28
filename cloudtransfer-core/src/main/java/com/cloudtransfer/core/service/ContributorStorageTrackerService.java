package com.cloudtransfer.core.service;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContributorStorageTrackerService {

    private final ContributorRepository contributorRepository;

    @Transactional
    public void decrementStorageUsage(UUID contributorId, long bytesToRelease) {
        if (contributorId == null) {
            return;
        }

        contributorRepository.findById(contributorId).ifPresent(contributor -> {
            long currentUsed = contributor.getUsedStorageBytes() != null ? contributor.getUsedStorageBytes() : 0L;
            long updatedUsed = Math.max(0L, currentUsed - bytesToRelease);
            contributor.setUsedStorageBytes(updatedUsed);
            contributorRepository.save(contributor);
            log.info("[StorageTracker] Released {} bytes for Contributor ID: {}. New Used: {} bytes",
                    bytesToRelease, contributorId, updatedUsed);
        });
    }

    @Transactional
    public void incrementStorageUsage(UUID contributorId, long bytesToConsume) {
        if (contributorId == null) {
            return;
        }

        contributorRepository.findById(contributorId).ifPresent(contributor -> {
            long currentUsed = contributor.getUsedStorageBytes() != null ? contributor.getUsedStorageBytes() : 0L;
            long updatedUsed = currentUsed + bytesToConsume;
            contributor.setUsedStorageBytes(updatedUsed);
            contributorRepository.save(contributor);
            log.info("[StorageTracker] Consumed {} bytes for Contributor ID: {}. New Used: {} bytes",
                    bytesToConsume, contributorId, updatedUsed);
        });
    }
}