package com.cloudtransfer.core.service;

import com.cloudtransfer.core.config.RabbitMQConfig;
import com.cloudtransfer.core.dto.BlueprintResponseDto;
import com.cloudtransfer.core.dto.ChunkAssignmentDto;
import com.cloudtransfer.core.dto.SessionInitMessage;
import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final ContributorRepository contributorRepository;
    private final TransferSessionRepository sessionRepository;
    private final FileChunkRepository chunkRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final long MAX_CHUNK_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final long MAX_VOLUNTEER_LOAD_BYTES = 1024 * 1024 * 1024; // 1 GB Circuit Breaker

    @Transactional
    public BlueprintResponseDto createBlueprint(String senderEmail, String filename, long totalSizeBytes, int expiryDays) {

        // 1. Calculate Chunks (50MB max chunk rule)
        int totalChunks = (int) Math.ceil((double) totalSizeBytes / MAX_CHUNK_SIZE_BYTES);
        if (totalChunks == 0) totalChunks = 1;

        // 2. Fetch Active Volunteers
        List<Contributor> activeContributors = contributorRepository.findActiveContributors();
        if (activeContributors.size() < 3) {
            throw new IllegalStateException("Not enough active volunteer drives available (Minimum 3 required).");
        }

        // 3. Dynamic Volunteer Allocation Formula: Max(3, Ceiling((Total File Size * 2) / 100MB))
        long primaryPlusReplicaBytes = totalSizeBytes * 2;
        long targetPerVolunteerBytes = 100 * 1024 * 1024; // 100MB target load
        int requiredVolunteers = (int) Math.max(3, Math.ceil((double) primaryPlusReplicaBytes / targetPerVolunteerBytes));

        // Scale down to reality if we have fewer volunteers online
        int selectedVolunteerCount = Math.min(requiredVolunteers, activeContributors.size());
        List<Contributor> selectedContributors = activeContributors.subList(0, selectedVolunteerCount);

        // 4. Circuit Breaker Check (Max 1GB per volunteer)
        long calculatedLoadPerVolunteer = primaryPlusReplicaBytes / selectedVolunteerCount;
        if (calculatedLoadPerVolunteer > MAX_VOLUNTEER_LOAD_BYTES) {
            throw new IllegalStateException("Network capacity limit reached. Please try uploading a smaller file or try again later.");
        }

        // 5. Save Transfer Session
        TransferSession session = new TransferSession();
        session.setSenderEmail(senderEmail);
        session.setOriginalFilename(filename);
        session.setTotalSizeBytes(totalSizeBytes);
        session.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        session = sessionRepository.save(session);

        // 6. Generate Ring Replication Mapping (Blueprint)
        List<ChunkAssignmentDto> assignments = new ArrayList<>();
        List<FileChunk> dbChunks = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            Contributor primary = selectedContributors.get(i % selectedVolunteerCount);
            Contributor replica = selectedContributors.get((i + 1) % selectedVolunteerCount); // Ring step (+1)

            FileChunk chunk = new FileChunk();
            chunk.setSession(session);
            chunk.setChunkIndex(i);
            chunk.setPrimaryContributor(primary);
            chunk.setReplicaContributor(replica);
            dbChunks.add(chunk);

            assignments.add(new ChunkAssignmentDto(
                    i,
                    primary.getId(),
                    primary.getCloudProvider(),
                    primary.getRefreshToken(),
                    replica.getId(),
                    replica.getCloudProvider(),
                    replica.getRefreshToken()
            ));
        }

        chunkRepository.saveAll(dbChunks);
        SessionInitMessage initMessage = new SessionInitMessage(session.getId(), totalChunks);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                initMessage
        );

        return new BlueprintResponseDto(
                session.getId(),
                totalSizeBytes,
                totalChunks,
                assignments
        );
    }
}