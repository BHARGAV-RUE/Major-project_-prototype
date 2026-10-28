package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.dto.ChunkAssignmentDto;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import com.cloudtransfer.core.dto.StartTransferRequest;
import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.service.TransferService;
import com.cloudtransfer.core.service.TransferService.Blueprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Slf4j
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final FileChunkRepository fileChunkRepository;
    private final ContributorRepository contributorRepository;

    @PostMapping("/start")
    public ResponseEntity<?> startTransfer(@RequestBody StartTransferRequest request) {
        log.info("[TransferController] Initiating start transfer for sender: {}", request.getSenderEmail());
        TransferSession session = transferService.startTransfer(request);
        Blueprint blueprint = transferService.calculateBlueprint(request.getTotalSizeBytes());

        return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "sessionId", session.getId(),
                "status", session.getStatus(),
                "totalChunks", session.getTotalChunks(),
                "chunkSizeBytes", blueprint.chunkSizeBytes(),
                "expiryTimestamp", session.getExpiryTimestamp().toString()
        ));
    }

    @GetMapping("/{sessionId}/chunk-assignments")
    public ResponseEntity<List<CloudProviderTokenDTO>> getChunkAssignments(@PathVariable UUID sessionId) {
        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(sessionId);
        List<CloudProviderTokenDTO> tokens = transferService.generateChunkAssignments(sessionId, chunks.size());
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/{sessionId}/chunk-assignment")
    public ResponseEntity<?> getChunkAssignment(
            @PathVariable UUID sessionId,
            @RequestParam(value = "chunkIndex", defaultValue = "0") Integer chunkIndex) {

        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(sessionId);

        FileChunk chunk = chunks.stream()
                .filter(c -> c.getChunkIndex().equals(chunkIndex))
                .findFirst()
                .orElse(null);

        if (chunk == null) {
            return ResponseEntity.notFound().build();
        }

        ChunkAssignmentDto dto = new ChunkAssignmentDto();
        dto.setChunkIndex(chunkIndex);

        if (chunk.getPrimaryContributor() != null) {
            UUID primaryId = chunk.getPrimaryContributor().getId();
            if (primaryId != null) {
                dto.setPrimaryContributorId(primaryId);
            }
            dto.setPrimaryProvider(chunk.getPrimaryContributor().getCloudProvider());
            dto.setPrimaryRefreshToken(chunk.getPrimaryContributor().getRefreshToken());
        }

        if (chunk.getReplicaContributor() != null) {
            UUID replicaId = chunk.getReplicaContributor().getId();
            if (replicaId != null) {
                dto.setReplicaContributorId(replicaId);
            }
            dto.setReplicaProvider(chunk.getReplicaContributor().getCloudProvider());
            dto.setReplicaRefreshToken(chunk.getReplicaContributor().getRefreshToken());
        }

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeTransfer(@PathVariable UUID sessionId) {
        log.info("[TransferController] Executing synchronous complete transfer for session {}", sessionId);
        TransferSession session = transferService.completeTransfer(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "status", session.getStatus(),
                "merkleRoot", session.getMerkleRoot() != null ? session.getMerkleRoot() : ""
        ));
    }

    // --- RECEIVES TAMPERED / MISSING CHUNK NOTIFICATION FROM STREAM ENGINE ---
    @PostMapping("/{sessionId}/chunks/{chunkIndex}/tampered")
    public ResponseEntity<?> reportTamperedChunk(
            @PathVariable UUID sessionId,
            @PathVariable Integer chunkIndex) {

        log.warn("[TamperDetection] Received missing/tampered chunk report for Session: {}, Chunk Index: {}", sessionId, chunkIndex);

        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(sessionId);

        Optional<FileChunk> chunkOpt = chunks.stream()
                .filter(c -> c.getChunkIndex().equals(chunkIndex))
                .findFirst();

        if (chunkOpt.isPresent()) {
            FileChunk chunk = chunkOpt.get();
            chunk.setStatus("MISSING");
            fileChunkRepository.save(chunk);

            // Increment tamper warning flags on the primary contributor node
            if (chunk.getPrimaryContributor() != null) {
                Contributor primary = chunk.getPrimaryContributor();
                int currentFlags = (primary.getTamperFlags() != null ? primary.getTamperFlags() : 0) + 1;
                primary.setTamperFlags(currentFlags);
                contributorRepository.save(primary);

                log.info("[TamperDetection] Incremented tamper warning flags for primary contributor node {}: Total Flags = {}",
                        primary.getEmail(), currentFlags);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "TAMPER_FLAGGED",
                    "sessionId", sessionId,
                    "chunkIndex", chunkIndex
            ));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Chunk not found for session " + sessionId));
    }
}