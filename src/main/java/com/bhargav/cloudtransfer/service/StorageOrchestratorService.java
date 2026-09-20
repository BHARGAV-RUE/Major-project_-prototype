package com.bhargav.cloudtransfer.service;

import com.bhargav.cloudtransfer.entity.Contributor;
import com.bhargav.cloudtransfer.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageOrchestratorService {

    private final ContributorRepository contributorRepository;

    /**
     * Finds the best contributor for a specific chunk based on available storage.
     */
    public Contributor selectBestContributorForChunk(long chunkSizeBytes) {
        List<Contributor> activeContributors = contributorRepository.findAll().stream()
                .filter(Contributor::isActive)
                .filter(c -> c.getAvailableSpace() >= chunkSizeBytes) // Must have enough room
                .sorted(Comparator.comparing(Contributor::getAvailableSpace).reversed()) // Largest space first
                .toList();

        if (activeContributors.isEmpty()) {
            throw new RuntimeException("Insufficient distributed storage capacity across all active contributors.");
        }

        // Select the contributor with the highest available storage
        return activeContributors.get(0);
    }

    /**
     * Updates the local database tracking after a successful upload.
     */
    public void commitStorageUsage(Contributor contributor, long chunkSizeBytes) {
        contributor.setUsedCapacityBytes(contributor.getUsedCapacityBytes() + chunkSizeBytes);
        contributorRepository.save(contributor);
    }
}