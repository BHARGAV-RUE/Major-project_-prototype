package com.cloudtransfer.core.service;

import com.cloudtransfer.core.blockchain.MerkleTreeService;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import com.cloudtransfer.core.dto.StartTransferRequest;
import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferRecipient;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferRecipientRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferService {

    private final TransferSessionRepository transferSessionRepository;
    private final TransferRecipientRepository transferRecipientRepository;
    private final FileChunkRepository fileChunkRepository;
    private final ConsistentHashRingService consistentHashRingService;
    private final MerkleTreeService merkleTreeService;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cloudtransfer.stream.url:http://localhost:8081/api/v1}")
    private String streamEngineBaseUrl;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public record Blueprint(int totalChunks, int chunkSizeBytes) {}

    public Blueprint calculateBlueprint(long totalSizeBytes) {
        int chunkSizeBytes = 50 * 1024 * 1024; // 50MB Slices
        int totalChunks = (int) Math.ceil((double) totalSizeBytes / chunkSizeBytes);
        if (totalChunks < 1) totalChunks = 1;
        return new Blueprint(totalChunks, chunkSizeBytes);
    }

    @Transactional
    public TransferSession startTransfer(StartTransferRequest request) {
        log.info("[TransferService] Starting new transfer session for sender: {}", request.getSenderEmail());

        Blueprint blueprint = calculateBlueprint(request.getTotalSizeBytes());

        TransferSession session = TransferSession.builder()
                .senderEmail(request.getSenderEmail())
                .senderName(request.getSenderName())
                .subject(request.getSubject())
                .message(request.getMessage())
                .originalFilename(request.getFilename())
                .totalSizeBytes(request.getTotalSizeBytes())
                .totalChunks(blueprint.totalChunks())
                .status("IN_PROGRESS")
                .createdAt(LocalDateTime.now())
                .expiryTimestamp(LocalDateTime.now().plusDays(request.getExpiryDays() != null && request.getExpiryDays() > 0 ? request.getExpiryDays() : 7))
                .build();

        session = transferSessionRepository.save(session);

        // Save Recipients
        if (request.getRecipientEmails() != null) {
            for (String email : request.getRecipientEmails()) {
                TransferRecipient recipient = TransferRecipient.builder()
                        .transferSession(session)
                        .recipientEmail(email)
                        .build();
                transferRecipientRepository.save(recipient);
            }
        }

        // Assign Volunteers and Save Chunks
        List<CloudProviderTokenDTO> tokenAssignments = new ArrayList<>();
        for (int i = 0; i < blueprint.totalChunks(); i++) {
            Contributor primary = consistentHashRingService.getPrimaryNode(session.getId().toString(), i);
            Contributor replica = consistentHashRingService.getReplicaNode(session.getId().toString(), i);

            FileChunk chunk = FileChunk.builder()
                    .session(session)
                    .chunkIndex(i)
                    .chunkSizeBytes((long) blueprint.chunkSizeBytes())
                    .primaryContributor(primary)
                    .replicaContributor(replica)
                    .build();

            fileChunkRepository.save(chunk);

            if (primary != null) {
                CloudProviderTokenDTO primaryToken = new CloudProviderTokenDTO();
                primaryToken.setChunkIndex(i);
                primaryToken.setProviderName(primary.getCloudProvider());
                primaryToken.setRefreshToken(primary.getRefreshToken());
                primaryToken.setReplica(false);
                tokenAssignments.add(primaryToken);
            }
            if (replica != null) {
                CloudProviderTokenDTO replicaToken = new CloudProviderTokenDTO();
                replicaToken.setChunkIndex(i);
                replicaToken.setProviderName(replica.getCloudProvider());
                replicaToken.setRefreshToken(replica.getRefreshToken());
                replicaToken.setReplica(true);
                tokenAssignments.add(replicaToken);
            }
        }

        // Synchronous Handshake: Pre-register session routing and tokens with Stream Engine
        registerSessionWithStreamEngine(session.getId().toString(), session.getTotalChunks(), tokenAssignments);

        return session;
    }

    public List<CloudProviderTokenDTO> generateChunkAssignments(UUID sessionId, int totalChunks) {
        List<CloudProviderTokenDTO> tokenAssignments = new ArrayList<>();
        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(sessionId);

        for (FileChunk chunk : chunks) {
            if (chunk.getPrimaryContributor() != null) {
                CloudProviderTokenDTO primaryToken = new CloudProviderTokenDTO();
                primaryToken.setChunkIndex(chunk.getChunkIndex());
                primaryToken.setProviderName(chunk.getPrimaryContributor().getCloudProvider());
                primaryToken.setRefreshToken(chunk.getPrimaryContributor().getRefreshToken());
                primaryToken.setReplica(false);
                tokenAssignments.add(primaryToken);
            }
            if (chunk.getReplicaContributor() != null) {
                CloudProviderTokenDTO replicaToken = new CloudProviderTokenDTO();
                replicaToken.setChunkIndex(chunk.getChunkIndex());
                replicaToken.setProviderName(chunk.getReplicaContributor().getCloudProvider());
                replicaToken.setRefreshToken(chunk.getReplicaContributor().getRefreshToken());
                replicaToken.setReplica(true);
                tokenAssignments.add(replicaToken);
            }
        }

        return tokenAssignments;
    }

    @Transactional
    public TransferSession completeTransfer(UUID sessionId) {
        TransferSession session = transferSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        log.info("[TransferService] Completing session {} synchronously...", sessionId);

        // Fetch buffered SHA-256 hashes directly from Stream Engine over REST
        List<Map<String, Object>> bufferedHashes = fetchBufferedHashesFromStreamEngine(sessionId.toString());

        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(sessionId);
        List<String> chunkHashes = new ArrayList<>();

        if (bufferedHashes != null && !bufferedHashes.isEmpty()) {
            for (Map<String, Object> hashData : bufferedHashes) {
                Integer chunkIndex = (Integer) hashData.get("chunkIndex");
                String sha256Hash = (String) hashData.get("sha256Hash");
                String primaryCloudFileId = (String) hashData.get("primaryCloudFileId");

                FileChunk chunk = chunks.stream()
                        .filter(c -> c.getChunkIndex().equals(chunkIndex))
                        .findFirst()
                        .orElse(null);

                if (chunk != null) {
                    chunk.setSha256Hash(sha256Hash);
                    chunk.setCloudFileId(primaryCloudFileId);
                    chunk.setStatus("UPLOADED");
                    fileChunkRepository.save(chunk);
                }
                if (sha256Hash != null && !sha256Hash.isBlank()) {
                    chunkHashes.add(sha256Hash);
                }
            }
        }

        // Generate Merkle Root & Anchor to Polygon Amoy Blockchain
        if (!chunkHashes.isEmpty() && session.getMerkleRoot() == null) {
            log.info("[TransferService] Generating Merkle Tree for {} hashes in session {}", chunkHashes.size(), sessionId);
            String merkleRoot = merkleTreeService.processAllChunks(sessionId.toString(), chunkHashes);
            session.setMerkleRoot(merkleRoot);
            session.setStatus("COMPLETED");
            session = transferSessionRepository.save(session);
        } else {
            session.setStatus("COMPLETED");
            session = transferSessionRepository.save(session);
        }

        // Dispatch Email Events for recipients & sender asynchronously
        String downloadUrl = frontendBaseUrl + "/download/" + session.getId();
        List<String> recipientEmailList = new ArrayList<>();

        if (session.getRecipients() != null) {
            for (TransferRecipient recipient : session.getRecipients()) {
                recipientEmailList.add(recipient.getRecipientEmail());
                if (!Boolean.TRUE.equals(recipient.getNotified())) {
                    eventPublisher.publishEvent(EmailEvent.builder()
                            .source(this)
                            .eventType(EmailEvent.EventType.TRANSFER_COMPLETE_RECIPIENT)
                            .recipientEmail(recipient.getRecipientEmail())
                            .senderName(session.getSenderName())
                            .senderEmail(session.getSenderEmail())
                            .subject(session.getSubject())
                            .message(session.getMessage())
                            .actionUrl(downloadUrl)
                            .originalFilename(session.getOriginalFilename())
                            .totalSizeBytes(session.getTotalSizeBytes())
                            .merkleRoot(session.getMerkleRoot())
                            .build());
                    recipient.setNotified(true);
                }
            }
        }

        // Notify Sender
        if (session.getSenderEmail() != null && !session.getSenderEmail().isBlank()) {
            eventPublisher.publishEvent(EmailEvent.builder()
                    .source(this)
                    .eventType(EmailEvent.EventType.TRANSFER_COMPLETE_SENDER)
                    .recipientEmail(session.getSenderEmail())
                    .senderName(session.getSenderName())
                    .originalFilename(session.getOriginalFilename())
                    .totalSizeBytes(session.getTotalSizeBytes())
                    .merkleRoot(session.getMerkleRoot())
                    .recipientEmails(recipientEmailList)
                    .build());
        }

        return session;
    }

    private void registerSessionWithStreamEngine(String sessionId, int totalChunks, List<CloudProviderTokenDTO> tokens) {
        try {
            String url = streamEngineBaseUrl + "/stream/session/register";
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", sessionId);
            payload.put("totalChunks", totalChunks);
            payload.put("tokens", tokens);

            restTemplate.postForEntity(url, payload, Map.class);
            log.info("[TransferService] Pre-registered session {} with Stream Engine successfully.", sessionId);
        } catch (Exception e) {
            log.error("[TransferService] Failed to pre-register session {} with Stream Engine: {}", sessionId, e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> fetchBufferedHashesFromStreamEngine(String sessionId) {
        try {
            String url = streamEngineBaseUrl + "/stream/session/" + sessionId + "/hashes";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("[TransferService] Could not fetch hashes from Stream Engine for session {}: {}", sessionId, e.getMessage());
            return new ArrayList<>();
        }
    }
}