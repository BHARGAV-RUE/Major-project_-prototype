package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final TransferSessionRepository transferSessionRepository;
    private final FileChunkRepository fileChunkRepository;

    @GetMapping("/download/{sessionId}")
    public ResponseEntity<?> getDownloadManifest(@PathVariable UUID sessionId) {
        Optional<TransferSession> sessionOpt = transferSessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", "Transfer session not found: " + sessionId));
        }

        TransferSession session = sessionOpt.get();

        // Check if session was deleted by Admin or force purged
        if ("ADMIN_DELETED".equalsIgnoreCase(session.getStatus()) || "FORCE_PURGED".equalsIgnoreCase(session.getStatus())) {
            log.warn("[DownloadController] Download blocked for session {}: Admin deleted files.", sessionId);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
                            "error", "Files Unavailable",
                            "message", "Admin deleted your files.",
                            "status", "ADMIN_DELETED"
                    ));
        }

        // Check Emergency Purge Status
        if ("EMERGENCY_PURGED".equalsIgnoreCase(session.getStatus())) {
            log.warn("[DownloadController] Download blocked for session {}: Emergency purged.", sessionId);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
                            "error", "Files Unavailable",
                            "message", "Admin deleted your files during a network emergency purge.",
                            "status", "EMERGENCY_PURGED"
                    ));
        }

        // Check Expiry
        if ("EXPIRED".equalsIgnoreCase(session.getStatus()) ||
                (session.getExpiryTimestamp() != null && LocalDateTime.now().isAfter(session.getExpiryTimestamp()))) {
            log.warn("[DownloadController] Download blocked for session {}: Session expired.", sessionId);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
                            "error", "Transfer Expired",
                            "message", "This file transfer session has expired and is no longer available.",
                            "status", "EXPIRED"
                    ));
        }

        List<FileChunk> chunks = fileChunkRepository.findBySessionIdOrderByChunkIndexAsc(sessionId);

        log.info("[Brain] Download manifest requested for session: {} (File: {}, Chunks: {}, Size: {} bytes)",
                sessionId, session.getOriginalFilename(), chunks.size(), session.getTotalSizeBytes());

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("sessionId", session.getId().toString());
        manifest.put("filename", session.getOriginalFilename() != null ? session.getOriginalFilename() : "CloudTransfer_Files.zip");
        manifest.put("originalFilename", session.getOriginalFilename() != null ? session.getOriginalFilename() : "CloudTransfer_Files.zip");
        manifest.put("totalSizeBytes", session.getTotalSizeBytes() != null ? session.getTotalSizeBytes() : 0L);
        manifest.put("totalChunks", chunks.size());
        manifest.put("senderName", session.getSenderName() != null ? session.getSenderName() : "Anonymous");
        manifest.put("senderEmail", session.getSenderEmail() != null ? session.getSenderEmail() : "");
        manifest.put("subject", session.getSubject() != null ? session.getSubject() : "");
        manifest.put("message", session.getMessage() != null ? session.getMessage() : "");
        manifest.put("status", session.getStatus());
        manifest.put("createdAt", session.getCreatedAt());
        manifest.put("expiryTimestamp", session.getExpiryTimestamp());
        manifest.put("merkleRoot", session.getMerkleRoot() != null ? session.getMerkleRoot() : "");

        return ResponseEntity.ok(manifest);
    }
}