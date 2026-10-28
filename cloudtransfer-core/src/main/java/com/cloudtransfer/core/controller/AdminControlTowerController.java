package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.dto.AdminChunkDetailDto;
import com.cloudtransfer.core.dto.AdminTransferSessionDto;
import com.cloudtransfer.core.dto.TamperAlertDto;
import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferRecipient;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminControlTowerController {

    private final ContributorRepository contributorRepository;
    private final TransferSessionRepository transferSessionRepository;
    private final FileChunkRepository fileChunkRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.admin.password:AdminSecret123!}")
    private String adminPassword;

    @Data
    public static class AdminLoginRequest {
        private String password;
    }

    @Data
    public static class SenderPurgeRequest {
        private String senderEmail;
    }

    private boolean isAuthorized(String headerSecret) {
        return headerSecret != null && headerSecret.equals(adminPassword);
    }

    // --- 1. ADMIN LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        if (request.getPassword() != null && request.getPassword().equals(adminPassword)) {
            return ResponseEntity.ok(Map.of("status", "AUTHENTICATED", "adminToken", adminPassword));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }

    // --- 2. TELEMETRY & ALERTS COUNT ---
    @GetMapping("/telemetry")
    public ResponseEntity<?> getSystemTelemetry(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Contributor> contributors = contributorRepository.findAll();
        long totalCapacity = contributors.stream().mapToLong(c -> c.getPledgedStorageBytes() != null ? c.getPledgedStorageBytes() : 0L).sum();
        long consumedStorage = contributors.stream().mapToLong(c -> c.getUsedStorageBytes() != null ? c.getUsedStorageBytes() : 0L).sum();
        long activeTransfers = transferSessionRepository.count();
        List<TamperAlertDto> alerts = fetchCombinedNotificationsInternal();

        return ResponseEntity.ok(Map.of(
                "activeContributors", contributors.size(),
                "totalCapacityBytes", totalCapacity,
                "consumedStorageBytes", consumedStorage,
                "activeTransfersCount", activeTransfers,
                "activeAlertsCount", alerts.size()
        ));
    }

    // --- 3. GET ALL CONTRIBUTORS ---
    @GetMapping("/contributors")
    public ResponseEntity<?> getAllContributors(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(contributorRepository.findAll());
    }

    // --- 4. APPROVE CONTRIBUTOR ---
    @PostMapping("/contributors/{id}/approve")
    public ResponseEntity<?> approveContributor(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Contributor> contributorOpt = contributorRepository.findById(id);
        if (contributorOpt.isEmpty()) return ResponseEntity.notFound().build();

        Contributor contributor = contributorOpt.get();
        contributor.setStatus("ACTIVE");
        contributorRepository.save(contributor);

        return ResponseEntity.ok(Map.of("message", "Contributor node approved and activated."));
    }

    // --- 5. FLAG CONTRIBUTOR FOR TAMPERING ---
    @PostMapping("/contributors/{id}/warn-tamper")
    public ResponseEntity<?> warnContributorTamper(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Contributor> contributorOpt = contributorRepository.findById(id);
        if (contributorOpt.isEmpty()) return ResponseEntity.notFound().build();

        Contributor contributor = contributorOpt.get();
        int currentFlags = (contributor.getTamperFlags() != null ? contributor.getTamperFlags() : 0) + 1;
        contributor.setTamperFlags(currentFlags);

        String contributorName = contributor.getName() != null ? contributor.getName() : contributor.getEmail();

        if (currentFlags <= 3) {
            contributorRepository.save(contributor);

            eventPublisher.publishEvent(EmailEvent.builder()
                    .source(this)
                    .eventType(EmailEvent.EventType.TAMPER_WARNING)
                    .recipientEmail(contributor.getEmail())
                    .recipientName(contributorName)
                    .cloudProvider(contributor.getCloudProvider())
                    .currentFlags(currentFlags)
                    .build());

            return ResponseEntity.ok(Map.of("message", "Warning flag #" + currentFlags + " issued.", "tamperFlags", currentFlags));
        } else {
            LocalDateTime scheduledTermination = addBusinessDays(LocalDateTime.now(), 4);
            contributor.setStatus("PENDING_TERMINATION");
            contributor.setScheduledTerminationAt(scheduledTermination);
            contributorRepository.save(contributor);

            eventPublisher.publishEvent(EmailEvent.builder()
                    .source(this)
                    .eventType(EmailEvent.EventType.TERMINATION_NOTICE_SCHEDULED)
                    .recipientEmail(contributor.getEmail())
                    .recipientName(contributorName)
                    .cloudProvider(contributor.getCloudProvider())
                    .terminationDate(scheduledTermination)
                    .build());

            return ResponseEntity.ok(Map.of(
                    "message", "Contributor reached " + currentFlags + " flags. Scheduled for termination in 4 business days.",
                    "tamperFlags", currentFlags,
                    "scheduledTerminationAt", scheduledTermination
            ));
        }
    }

    // --- 6. TERMINATE CONTRIBUTOR NODE ---
    @DeleteMapping("/contributors/{id}")
    public ResponseEntity<?> deleteContributor(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Contributor> contributorOpt = contributorRepository.findById(id);
        if (contributorOpt.isEmpty()) return ResponseEntity.notFound().build();

        Contributor contributor = contributorOpt.get();

        List<FileChunk> primaryChunks = fileChunkRepository.findByPrimaryContributor_IdOrderByChunkIndexAsc(id);
        for (FileChunk chunk : primaryChunks) {
            chunk.setPrimaryContributor(null);
            chunk.setStatus("FAILOVER_REQUIRED");
        }
        fileChunkRepository.saveAll(primaryChunks);

        List<FileChunk> replicaChunks = fileChunkRepository.findByReplicaContributor_IdOrderByChunkIndexAsc(id);
        for (FileChunk chunk : replicaChunks) {
            chunk.setReplicaContributor(null);
        }
        fileChunkRepository.saveAll(replicaChunks);

        contributorRepository.deleteById(id);

        eventPublisher.publishEvent(EmailEvent.builder()
                .source(this)
                .eventType(EmailEvent.EventType.IMMEDIATE_TERMINATION)
                .recipientEmail(contributor.getEmail())
                .recipientName(contributor.getName())
                .cloudProvider(contributor.getCloudProvider())
                .message("Manual Admin purge due to chunk data tampering or security policy violations.")
                .build());

        return ResponseEntity.ok(Map.of("message", "Contributor node terminated immediately."));
    }

    // --- 7. DETAILED TRANSFER SESSIONS (SORTED MOST RECENT TO PAST) ---
    @GetMapping("/transfers")
    public ResponseEntity<?> getAllTransfers(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<TransferSession> sessions = transferSessionRepository.findAll();

        List<AdminTransferSessionDto> dtos = sessions.stream()
                .sorted((s1, s2) -> {
                    if (s1.getCreatedAt() == null || s2.getCreatedAt() == null) return 0;
                    return s2.getCreatedAt().compareTo(s1.getCreatedAt()); // Most recent first
                })
                .map(s -> {
                    List<String> recipientEmails = s.getRecipients() != null
                            ? s.getRecipients().stream().map(TransferRecipient::getRecipientEmail).collect(Collectors.toList())
                            : Collections.emptyList();

                    List<AdminChunkDetailDto> chunkDtos = s.getChunks() != null
                            ? s.getChunks().stream().map(c -> AdminChunkDetailDto.builder()
                            .id(c.getId())
                            .chunkIndex(c.getChunkIndex())
                            .chunkSizeBytes(c.getChunkSizeBytes())
                            .sha256Hash(c.getSha256Hash())
                            .status(c.getStatus() != null ? c.getStatus() : "HEALTHY")
                            .primaryContributorId(c.getPrimaryContributor() != null ? c.getPrimaryContributor().getId() : null)
                            .primaryContributorEmail(c.getPrimaryContributor() != null ? c.getPrimaryContributor().getEmail() : "UNASSIGNED")
                            .primaryProvider(c.getPrimaryContributor() != null ? c.getPrimaryContributor().getCloudProvider() : "N/A")
                            .replicaContributorId(c.getReplicaContributor() != null ? c.getReplicaContributor().getId() : null)
                            .replicaContributorEmail(c.getReplicaContributor() != null ? c.getReplicaContributor().getEmail() : "UNASSIGNED")
                            .replicaProvider(c.getReplicaContributor() != null ? c.getReplicaContributor().getCloudProvider() : "N/A")
                            .build()).collect(Collectors.toList())
                            : Collections.emptyList();

                    boolean isTampered = chunkDtos.stream().anyMatch(c -> "TAMPERED".equalsIgnoreCase(c.getStatus()) || "MISSING".equalsIgnoreCase(c.getStatus()));
                    String blockchainStatus = (s.getMerkleRoot() != null && !s.getMerkleRoot().isBlank() && !isTampered) ? "ANCHORED" : "ON_CHAIN_MISMATCH";

                    return AdminTransferSessionDto.builder()
                            .id(s.getId())
                            .senderEmail(s.getSenderEmail())
                            .senderName(s.getSenderName())
                            .subject(s.getSubject())
                            .message(s.getMessage())
                            .originalFilename(s.getOriginalFilename())
                            .totalSizeBytes(s.getTotalSizeBytes())
                            .totalChunks(s.getTotalChunks())
                            .expiryDays(s.getExpiryDays())
                            .createdAt(s.getCreatedAt())
                            .expiryTimestamp(s.getExpiryTimestamp())
                            .status(s.getStatus())
                            .merkleRoot(s.getMerkleRoot())
                            .recipientEmails(recipientEmails)
                            .chunks(chunkDtos)
                            .blockchainStatus(blockchainStatus)
                            .build();
                }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // --- 8. MANUAL ADMIN DELETION OF A SPECIFIC TRANSFER SESSION ---
    @DeleteMapping("/transfers/{id}")
    @Transactional
    public ResponseEntity<?> deleteTransferSession(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<TransferSession> sessionOpt = transferSessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransferSession session = sessionOpt.get();
        List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(session.getId());

        long reclaimedBytes = 0L;
        for (FileChunk chunk : chunks) {
            long size = chunk.getChunkSizeBytes() != null ? chunk.getChunkSizeBytes() : 0L;
            reclaimedBytes += size;

            if (chunk.getPrimaryContributor() != null) {
                Contributor primary = chunk.getPrimaryContributor();
                long currentUsed = primary.getUsedStorageBytes() != null ? primary.getUsedStorageBytes() : 0L;
                primary.setUsedStorageBytes(Math.max(0L, currentUsed - size));
                contributorRepository.save(primary);
            }

            if (chunk.getReplicaContributor() != null) {
                Contributor replica = chunk.getReplicaContributor();
                long currentUsed = replica.getUsedStorageBytes() != null ? replica.getUsedStorageBytes() : 0L;
                replica.setUsedStorageBytes(Math.max(0L, currentUsed - size));
                contributorRepository.save(replica);
            }
        }

        fileChunkRepository.deleteAll(chunks);
        session.setStatus("ADMIN_DELETED");
        transferSessionRepository.save(session);

        log.info("[AdminControlTower] Session {} manually deleted by Admin. Reclaimed {} bytes across {} chunks.",
                id, reclaimedBytes, chunks.size());

        return ResponseEntity.ok(Map.of(
                "message", "Transfer session and associated chunks deleted by Admin.",
                "sessionId", id,
                "reclaimedBytes", reclaimedBytes,
                "status", "ADMIN_DELETED"
        ));
    }

    // --- 9. NOTIFICATIONS FEED & ALERTS ---
    @GetMapping("/tamper-alerts")
    public ResponseEntity<?> getTamperAlerts(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(fetchCombinedNotificationsInternal());
    }

    // --- 10. PURGE ALL TRANSFERS FOR A SPECIFIC SENDER EMAIL ---
    @DeleteMapping("/transfers/purge-sender")
    @Transactional
    public ResponseEntity<?> purgeSenderTransfers(
            @RequestParam("email") String senderEmail,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (senderEmail == null || senderEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender email is required."));
        }

        List<TransferSession> senderSessions = transferSessionRepository.findAll().stream()
                .filter(s -> senderEmail.equalsIgnoreCase(s.getSenderEmail()))
                .toList();

        if (senderSessions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active transfers found for sender: " + senderEmail));
        }

        long totalReclaimedBytes = 0L;
        int deletedChunkCount = 0;

        for (TransferSession session : senderSessions) {
            List<FileChunk> chunks = fileChunkRepository.findBySession_IdOrderByChunkIndexAsc(session.getId());
            for (FileChunk chunk : chunks) {
                long size = chunk.getChunkSizeBytes() != null ? chunk.getChunkSizeBytes() : 0L;
                totalReclaimedBytes += size;
                deletedChunkCount++;

                if (chunk.getPrimaryContributor() != null) {
                    Contributor primary = chunk.getPrimaryContributor();
                    long currentUsed = primary.getUsedStorageBytes() != null ? primary.getUsedStorageBytes() : 0L;
                    primary.setUsedStorageBytes(Math.max(0L, currentUsed - size));
                    contributorRepository.save(primary);
                }

                if (chunk.getReplicaContributor() != null) {
                    Contributor replica = chunk.getReplicaContributor();
                    long currentUsed = replica.getUsedStorageBytes() != null ? replica.getUsedStorageBytes() : 0L;
                    replica.setUsedStorageBytes(Math.max(0L, currentUsed - size));
                    contributorRepository.save(replica);
                }
            }
            fileChunkRepository.deleteAll(chunks);
            session.setStatus("PURGED_BY_ADMIN");
            transferSessionRepository.save(session);
        }

        log.warn("[AdminControlTower] FORCE PURGE executed for sender {}. Reclaimed {} bytes across {} session(s).",
                senderEmail, totalReclaimedBytes, senderSessions.size());

        return ResponseEntity.ok(Map.of(
                "message", "Successfully purged all transfers for sender " + senderEmail,
                "senderEmail", senderEmail,
                "purgedSessions", senderSessions.size(),
                "deletedChunks", deletedChunkCount,
                "reclaimedBytes", totalReclaimedBytes
        ));
    }

    // --- 11. TRIGGER SYSTEM-WIDE EMERGENCY PURGE ---
    @PostMapping("/transfers/emergency-purge")
    @Transactional
    public ResponseEntity<?> triggerEmergencySystemPurge(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {

        if (!isAuthorized(adminSecret)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<FileChunk> allChunks = fileChunkRepository.findAll();
        long totalReclaimedBytes = allChunks.stream()
                .mapToLong(c -> c.getChunkSizeBytes() != null ? c.getChunkSizeBytes() : 0L)
                .sum();

        fileChunkRepository.deleteAll();

        // Reset used storage counters across all contributors
        List<Contributor> allContributors = contributorRepository.findAll();
        for (Contributor c : allContributors) {
            c.setUsedStorageBytes(0L);
        }
        contributorRepository.saveAll(allContributors);

        // Mark all transfer sessions as PURGED
        List<TransferSession> allSessions = transferSessionRepository.findAll();
        for (TransferSession s : allSessions) {
            s.setStatus("SYSTEM_EMERGENCY_PURGED");
        }
        transferSessionRepository.saveAll(allSessions);

        log.error("[AdminControlTower] 🚨 EMERGENCY SYSTEM PURGE EXECUTED! All chunks wiped, contributor storage counters reset.");

        return ResponseEntity.ok(Map.of(
                "message", "Emergency system purge complete. All stored file chunks deleted and network capacity reset.",
                "totalReclaimedBytes", totalReclaimedBytes,
                "purgedSessionsCount", allSessions.size()
        ));
    }

    private List<TamperAlertDto> fetchCombinedNotificationsInternal() {
        List<TamperAlertDto> alerts = new ArrayList<>();

        // 1. Pending registration requests
        List<Contributor> inactives = contributorRepository.findAll().stream()
                .filter(c -> "INACTIVE".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        for (Contributor inactive : inactives) {
            alerts.add(TamperAlertDto.builder()
                    .alertId("REG-" + inactive.getId().toString().substring(0, 8))
                    .contributorId(inactive.getId())
                    .contributorEmail(inactive.getEmail())
                    .cloudProvider(inactive.getCloudProvider())
                    .tamperType("NEW_REGISTRATION_REQUEST")
                    .tamperFlags(inactive.getTamperFlags())
                    .timestamp(inactive.getCreatedAt() != null ? inactive.getCreatedAt() : LocalDateTime.now())
                    .status("INACTIVE")
                    .build());
        }

        // 2. Missing or tampered chunk violations
        List<FileChunk> allChunks = fileChunkRepository.findAll();
        for (FileChunk chunk : allChunks) {
            if ("TAMPERED".equalsIgnoreCase(chunk.getStatus()) || "MISSING".equalsIgnoreCase(chunk.getStatus())) {
                Contributor primary = chunk.getPrimaryContributor();
                if (primary != null) {
                    alerts.add(TamperAlertDto.builder()
                            .alertId("ALERT-" + chunk.getId().toString().substring(0, 8))
                            .sessionId(chunk.getSession() != null ? chunk.getSession().getId() : null)
                            .chunkIndex(chunk.getChunkIndex())
                            .contributorId(primary.getId())
                            .contributorEmail(primary.getEmail())
                            .cloudProvider(primary.getCloudProvider())
                            .tamperType("DELETED_FILE_OR_TAMPERED")
                            .tamperFlags(primary.getTamperFlags() != null ? primary.getTamperFlags() : 0)
                            .timestamp(LocalDateTime.now())
                            .scheduledTerminationAt(primary.getScheduledTerminationAt())
                            .status(primary.getStatus())
                            .build());
                }
            }
        }

        // 3. Flagged contributors with active warning flags (> 0)
        List<Contributor> flaggedContributors = contributorRepository.findAll().stream()
                .filter(c -> c.getTamperFlags() != null && c.getTamperFlags() > 0)
                .collect(Collectors.toList());

        for (Contributor c : flaggedContributors) {
            boolean alreadyListed = alerts.stream()
                    .anyMatch(a -> a.getContributorId() != null && a.getContributorId().equals(c.getId()));
            if (!alreadyListed) {
                alerts.add(TamperAlertDto.builder()
                        .alertId("FLAG-" + c.getId().toString().substring(0, 8))
                        .contributorId(c.getId())
                        .contributorEmail(c.getEmail())
                        .cloudProvider(c.getCloudProvider())
                        .tamperType("WARNING_FLAGS_ISSUED")
                        .tamperFlags(c.getTamperFlags())
                        .timestamp(LocalDateTime.now())
                        .status("WARNING_ACTIVE")
                        .build());
            }
        }

        return alerts;
    }

    private LocalDateTime addBusinessDays(LocalDateTime startDate, int businessDays) {
        LocalDateTime result = startDate;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}