package com.cloudtransfer.core.service;

import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsistentHashRingService {

    private final ContributorRepository contributorRepository;
    private static final int VIRTUAL_NODES_PER_CONTRIBUTOR = 100;
    private static final double SAFETY_THRESHOLD_RATIO = 0.90; // Stop at 90% (e.g., 9GB out of 10GB)
    private static final long ESTIMATED_CHUNK_SIZE_BYTES = 52_428_800L; // 50 MB (matches TransferService)
    private static final long DEFAULT_FALLBACK_PLEDGED_BYTES = 5_368_709_120L; // 5 GB default

    @Data
    @AllArgsConstructor
    public static class RingNodeAssignment {
        private Contributor primaryContributor;
        private Contributor replicaContributor;
    }

    public Contributor getPrimaryNode(String sessionId, int chunkIndex) {
        RingNodeAssignment assignment = getRingAssignmentForChunk(UUID.fromString(sessionId), chunkIndex);
        return assignment != null ? assignment.getPrimaryContributor() : null;
    }

    public Contributor getReplicaNode(String sessionId, int chunkIndex) {
        RingNodeAssignment assignment = getRingAssignmentForChunk(UUID.fromString(sessionId), chunkIndex);
        return assignment != null ? assignment.getReplicaContributor() : null;
    }

    public RingNodeAssignment getRingAssignmentForChunk(UUID sessionId, int chunkIndex) {
        List<Contributor> allContributors = contributorRepository.findAll();

        if (allContributors.isEmpty()) {
            log.warn("[ConsistentHashRing] Database has no volunteer contributors! Session {} chunk {} returning system fallback.",
                    sessionId, chunkIndex);
            return new RingNodeAssignment(null, null);
        }

        List<Contributor> eligibleContributors = allContributors.stream()
                .filter(c -> c.getStatus() == null || "ACTIVE".equalsIgnoreCase(c.getStatus()) || "APPROVED".equalsIgnoreCase(c.getStatus()))
                .filter(this::hasAvailableStorageCapacity)
                .toList();

        if (eligibleContributors.isEmpty()) {
            log.warn("[ConsistentHashRing] No active volunteers under 90% capacity limit! Falling back to all available volunteers.");
            eligibleContributors = allContributors.stream()
                    .filter(c -> !"TERMINATED".equalsIgnoreCase(c.getStatus()))
                    .toList();
        }

        if (eligibleContributors.isEmpty()) {
            log.warn("[ConsistentHashRing] Still no eligible volunteers found! Returning system fallback.");
            return new RingNodeAssignment(null, null);
        }

        if (eligibleContributors.size() == 1) {
            Contributor singleNode = eligibleContributors.getFirst();
            log.info("[ConsistentHashRing] Single volunteer available: {}", singleNode.getEmail());
            return new RingNodeAssignment(singleNode, singleNode);
        }

        NavigableMap<Long, Contributor> ring = new TreeMap<>();
        for (Contributor contributor : eligibleContributors) {
            String email = contributor.getEmail() != null ? contributor.getEmail() : "contributor_" + contributor.getId();
            for (int i = 0; i < VIRTUAL_NODES_PER_CONTRIBUTOR; i++) {
                String virtualNodeKey = email + "#VN#" + i;
                long hash = hash(virtualNodeKey);
                ring.put(hash, contributor);
            }
        }

        if (ring.isEmpty()) {
            return new RingNodeAssignment(null, null);
        }

        String chunkKey = sessionId.toString() + "_chunk_" + chunkIndex;
        long chunkHash = hash(chunkKey);

        Long primaryHashKey = ring.ceilingKey(chunkHash);
        if (primaryHashKey == null) {
            primaryHashKey = ring.firstKey();
        }
        Contributor primaryContributor = ring.get(primaryHashKey);

        Contributor replicaContributor = null;
        Long currentHashKey = primaryHashKey;
        int maxAttempts = ring.size();
        int attempts = 0;

        while (attempts < maxAttempts) {
            currentHashKey = ring.higherKey(currentHashKey);
            if (currentHashKey == null) {
                currentHashKey = ring.firstKey();
            }
            Contributor candidate = ring.get(currentHashKey);
            if (candidate != null && !Objects.equals(candidate.getId(), primaryContributor.getId())) {
                replicaContributor = candidate;
                break;
            }
            attempts++;
        }

        if (replicaContributor == null) {
            replicaContributor = primaryContributor;
        }

        long primaryUsed = primaryContributor.getUsedStorageBytes() != null ? primaryContributor.getUsedStorageBytes() : 0L;
        long primaryPledged = primaryContributor.getPledgedStorageBytes() != null && primaryContributor.getPledgedStorageBytes() > 0
                ? primaryContributor.getPledgedStorageBytes()
                : DEFAULT_FALLBACK_PLEDGED_BYTES;

        log.info("[ConsistentHashRing] Chunk #{} assigned -> Primary: {} (Used: {} MB / Max Usable: {} MB) | Replica: {}",
                chunkIndex,
                primaryContributor.getEmail(),
                (primaryUsed / (1024 * 1024)),
                (long) (primaryPledged * SAFETY_THRESHOLD_RATIO / (1024 * 1024)),
                replicaContributor.getEmail());

        return new RingNodeAssignment(primaryContributor, replicaContributor);
    }

    /**
     * Builds the CloudProviderTokenDTO token assignment for Stream Engine using Contributor entity.
     */
    public CloudProviderTokenDTO assignChunkTokens(int chunkIndex, Contributor primaryNode, Contributor replicaNode) {
        String primaryProvider = (primaryNode.getGoogleDriveRefreshToken() != null && !primaryNode.getGoogleDriveRefreshToken().isBlank())
                ? "GOOGLE_DRIVE"
                : "DROPBOX";

        String primaryRefreshToken = "GOOGLE_DRIVE".equalsIgnoreCase(primaryProvider)
                ? primaryNode.getGoogleDriveRefreshToken()
                : primaryNode.getDropboxRefreshToken();

        String replicaProvider = (replicaNode != null && replicaNode.getDropboxRefreshToken() != null && !replicaNode.getDropboxRefreshToken().isBlank())
                ? "DROPBOX"
                : "GOOGLE_DRIVE";

        String replicaRefreshToken = (replicaNode != null)
                ? ("DROPBOX".equalsIgnoreCase(replicaProvider) ? replicaNode.getDropboxRefreshToken() : replicaNode.getGoogleDriveRefreshToken())
                : null;

        return CloudProviderTokenDTO.builder()
                .chunkIndex(chunkIndex)
                .primaryVolunteerEmail(primaryNode.getEmail())
                .primaryProvider(primaryProvider)
                .primaryRefreshToken(primaryRefreshToken)
                .replicaVolunteerEmail(replicaNode != null ? replicaNode.getEmail() : null)
                .replicaProvider(replicaNode != null ? replicaProvider : null)
                .replicaRefreshToken(replicaRefreshToken)
                .build();
    }

    private boolean hasAvailableStorageCapacity(Contributor contributor) {
        long pledgedBytes = contributor.getPledgedStorageBytes() != null && contributor.getPledgedStorageBytes() > 0
                ? contributor.getPledgedStorageBytes()
                : DEFAULT_FALLBACK_PLEDGED_BYTES;
        long usedBytes = contributor.getUsedStorageBytes() != null ? contributor.getUsedStorageBytes() : 0L;

        long maxUsableBytes = (long) (pledgedBytes * SAFETY_THRESHOLD_RATIO);
        boolean hasSpace = (usedBytes + ESTIMATED_CHUNK_SIZE_BYTES) <= maxUsableBytes;

        if (!hasSpace) {
            log.warn("[CapacityCheck] Contributor {} REACHED 90% SAFETY CAP! (Used: {} MB, Max Allowed 90%: {} MB). Skipping from Ring.",
                    contributor.getEmail(),
                    (usedBytes / (1024 * 1024)),
                    (maxUsableBytes / (1024 * 1024)));
        }

        return hasSpace;
    }

    private long hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((long) (digest[3] & 0xFF) << 24) |
                    ((long) (digest[2] & 0xFF) << 16) |
                    ((long) (digest[1] & 0xFF) << 8) |
                    ((long) (digest[0] & 0xFF));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 digest algorithm not found", e);
        }
    }
}