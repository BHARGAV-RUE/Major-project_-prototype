package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.repository.ContributorRepository;
import com.cloudtransfer.core.repository.FileChunkRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/contributors")
@Slf4j
@RequiredArgsConstructor
public class ContributorOnboardingController {

    private final ContributorRepository contributorRepository;
    private final FileChunkRepository fileChunkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static final long MIN_PLEDGED_STORAGE_BYTES = 4L * 1024 * 1024 * 1024; // 4 GB Minimum

    @Data
    public static class ContributorRegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String cloudProvider;
        private Long pledgedStorageBytes;
        private String refreshToken;
    }

    @Data
    public static class ContributorLoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class ForfeitNodeRequest {
        private String password;
    }

    // --- 1. UNIFIED REGISTER / LINK PROVIDER NODE (PRE-OAUTH) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerContributor(@RequestBody ContributorRegistrationRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email address is required."));
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));
        }

        if (request.getPledgedStorageBytes() == null || request.getPledgedStorageBytes() < MIN_PLEDGED_STORAGE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Storage Amount",
                    "message", "Minimum pledged storage must be at least 4 GB."
            ));
        }

        String provider = request.getCloudProvider() != null ? request.getCloudProvider() : "GOOGLE_DRIVE";
        List<Contributor> existingUserNodes = contributorRepository.findAllByEmail(request.getEmail());

        String hashedPassword;
        if (!existingUserNodes.isEmpty()) {
            String existingHashedPassword = existingUserNodes.get(0).getPassword();
            if (existingHashedPassword != null && !passwordEncoder.matches(request.getPassword(), existingHashedPassword)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication Failed", "message", "Incorrect password for existing contributor account."));
            }
            hashedPassword = existingHashedPassword; // Synchronize with existing password
        } else {
            hashedPassword = passwordEncoder.encode(request.getPassword());
        }

        Optional<Contributor> existingNode = contributorRepository.findByEmailAndCloudProvider(request.getEmail(), provider);
        LocalDateTime now = LocalDateTime.now();

        Contributor contributor;
        if (existingNode.isPresent()) {
            contributor = existingNode.get();
            contributor.setName(request.getName() != null ? request.getName() : contributor.getName());
            contributor.setPledgedStorageBytes(request.getPledgedStorageBytes());
            contributor.setPassword(hashedPassword);
        } else {
            contributor = Contributor.builder()
                    .name(request.getName() != null && !request.getName().isBlank() ? request.getName() : request.getEmail())
                    .email(request.getEmail())
                    .password(hashedPassword)
                    .cloudProvider(provider)
                    .pledgedStorageBytes(request.getPledgedStorageBytes())
                    .usedStorageBytes(0L)
                    .refreshToken(request.getRefreshToken())
                    .status("PENDING_OAUTH") // Remains PENDING_OAUTH until OAuth completes
                    .tamperFlags(0)
                    .createdAt(now)
                    .build();
        }

        Contributor saved = contributorRepository.save(contributor);

        // Synchronize password across all linked nodes under this email
        for (Contributor node : existingUserNodes) {
            node.setPassword(hashedPassword);
        }
        if (!existingUserNodes.isEmpty()) {
            contributorRepository.saveAll(existingUserNodes);
        }

        return ResponseEntity.ok(saved);
    }

    // --- 2. AUTOMATIC 4-DAY ACTIVATION SCHEDULER ---
    @Scheduled(fixedRate = 60000)
    public void autoActivatePendingContributors() {
        List<Contributor> allContributors = contributorRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Contributor c : allContributors) {
            if ("INACTIVE".equalsIgnoreCase(c.getStatus())) {
                if (c.getAutoActivateAt() != null && now.isAfter(c.getAutoActivateAt())) {
                    c.setStatus("ACTIVE");
                    contributorRepository.save(c);
                    log.info("[AutoActivation] Contributor node ID {} ({}) auto-activated.", c.getId(), c.getEmail());
                }
            }
        }
    }

    // --- 3. UNIFIED SINGLE ACCOUNT DASHBOARD DETAILS ---
    @GetMapping("/dashboard/{email}")
    public ResponseEntity<?> getDashboardDetails(@PathVariable String email) {
        autoActivatePendingContributors();
        List<Contributor> nodes = contributorRepository.findAllByEmail(email);

        if (nodes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", "No contributor account found for email: " + email));
        }

        long totalPledged = nodes.stream().mapToLong(c -> c.getPledgedStorageBytes() != null ? c.getPledgedStorageBytes() : 0L).sum();
        long totalUsed = nodes.stream().mapToLong(c -> c.getUsedStorageBytes() != null ? c.getUsedStorageBytes() : 0L).sum();

        Optional<Contributor> googleNode = nodes.stream().filter(n -> "GOOGLE_DRIVE".equalsIgnoreCase(n.getCloudProvider())).findFirst();
        Optional<Contributor> dropboxNode = nodes.stream().filter(n -> "DROPBOX".equalsIgnoreCase(n.getCloudProvider())).findFirst();

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("name", nodes.get(0).getName());
        response.put("status", nodes.get(0).getStatus());
        response.put("pledgedStorageBytes", totalPledged);
        response.put("usedStorageBytes", totalUsed);
        response.put("googleDriveNode", googleNode.orElse(null));
        response.put("dropboxNode", dropboxNode.orElse(null));
        response.put("nodes", nodes);

        return ResponseEntity.ok(response);
    }

    // --- 4. UNIFIED DASHBOARD LOGIN ---
    @PostMapping("/dashboard/login")
    public ResponseEntity<?> loginDashboard(@RequestBody ContributorLoginRequest loginRequest) {
        autoActivatePendingContributors();
        List<Contributor> nodes = contributorRepository.findAllByEmail(loginRequest.getEmail());

        if (nodes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", "No contributor account found for email: " + loginRequest.getEmail()));
        }

        String storedHash = nodes.get(0).getPassword();
        if (storedHash != null && !passwordEncoder.matches(loginRequest.getPassword(), storedHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid email or password."));
        }

        return getDashboardDetails(loginRequest.getEmail());
    }

    // --- 5. FORFEIT NODE ---
    @PostMapping("/forfeit/{id}")
    public ResponseEntity<?> forfeitContributorNode(
            @PathVariable UUID id,
            @RequestBody ForfeitNodeRequest request) {

        Optional<Contributor> contributorOpt = contributorRepository.findById(id);
        if (contributorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Contributor contributor = contributorOpt.get();

        if (contributor.getPassword() != null && !passwordEncoder.matches(request.getPassword(), contributor.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid password. Forfeiture aborted."));
        }

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
                .eventType(EmailEvent.EventType.CONTRIBUTOR_FORFEITED)
                .recipientEmail(contributor.getEmail())
                .recipientName(contributor.getName())
                .cloudProvider(contributor.getCloudProvider())
                .build());

        return ResponseEntity.ok(Map.of("message", "Node successfully forfeited.", "nodeId", id));
    }
}