package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/contributors")
@Slf4j
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorRepository contributorRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String redirectUri;

    @Value("${dropbox.oauth.client-id:}")
    private String dropboxClientId;

    @Value("${dropbox.oauth.client-secret:}")
    private String dropboxClientSecret;

    @Value("${dropbox.oauth.redirect-uri:}")
    private String dropboxRedirectUri;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // --- GOOGLE OAUTH AUTH URL ---
    @GetMapping("/oauth/google/url")
    public ResponseEntity<String> getGoogleAuthUrl(@RequestParam(value = "email", required = false) String email) {
        String stateParam = (email != null && !email.isBlank()) ? email.trim() : "unknown";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/drive.file" +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + stateParam;

        return ResponseEntity.ok(authUrl);
    }

    // --- GOOGLE OAUTH CALLBACK ---
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<Void> handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("refresh_token")) {
                String refreshToken = (String) responseBody.get("refresh_token");
                String userEmail = (state != null && !state.isBlank() && !state.equals("unknown")) ? state : "contributor_" + System.currentTimeMillis() + "@example.com";

                Optional<Contributor> existingNode = contributorRepository.findByEmailAndCloudProvider(userEmail, "GOOGLE_DRIVE");
                List<Contributor> existingUserNodes = contributorRepository.findAllByEmail(userEmail);
                String existingPassword = !existingUserNodes.isEmpty() ? existingUserNodes.get(0).getPassword() : null;

                Contributor contributor;
                boolean shouldSendWelcomeEmail = false;

                if (existingNode.isPresent()) {
                    contributor = existingNode.get();
                    contributor.setRefreshToken(refreshToken);
                    if (existingPassword != null) contributor.setPassword(existingPassword);
                    if ("PENDING_OAUTH".equalsIgnoreCase(contributor.getStatus()) || contributor.getStatus() == null) {
                        contributor.setStatus("INACTIVE");
                        contributor.setAutoActivateAt(LocalDateTime.now().plusDays(4));
                        shouldSendWelcomeEmail = true;
                    }
                    log.info("[OAuthCallback] OAuth completed and Google Drive node updated for {}", userEmail);
                } else {
                    contributor = Contributor.builder()
                            .name(userEmail)
                            .email(userEmail)
                            .cloudProvider("GOOGLE_DRIVE")
                            .refreshToken(refreshToken)
                            .password(existingPassword)
                            .pledgedStorageBytes(5L * 1024 * 1024 * 1024)
                            .status("INACTIVE")
                            .tamperFlags(0)
                            .createdAt(LocalDateTime.now())
                            .autoActivateAt(LocalDateTime.now().plusDays(4))
                            .build();
                    shouldSendWelcomeEmail = true;
                    log.info("[OAuthCallback] OAuth completed and new Google Drive node created for {}", userEmail);
                }

                Contributor saved = contributorRepository.save(contributor);

                if (shouldSendWelcomeEmail) {
                    eventPublisher.publishEvent(EmailEvent.builder()
                            .source(this)
                            .eventType(EmailEvent.EventType.CONTRIBUTOR_REGISTERED)
                            .recipientEmail(saved.getEmail())
                            .recipientName(saved.getName() != null ? saved.getName() : saved.getEmail())
                            .cloudProvider(saved.getCloudProvider())
                            .pledgedBytes(saved.getPledgedStorageBytes())
                            .build());
                }

                String redirectUrl = frontendBaseUrl + "/contributor/register?status=success&provider=Google%20Drive";
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            } else {
                String redirectUrl = frontendBaseUrl + "/contributor/register?status=error&message=Failed%20to%20retrieve%20Google%20refresh%20token";
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            }

        } catch (Exception e) {
            log.error("[OAuthCallback] Google callback error: {}", e.getMessage(), e);
            String redirectUrl = frontendBaseUrl + "/contributor/register?status=error&message=" + e.getMessage();
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }
    }

    // --- DROPBOX OAUTH AUTH URL ---
    @GetMapping("/oauth/dropbox/url")
    public ResponseEntity<String> getDropboxAuthUrl(@RequestParam(value = "email", required = false) String email) {
        String stateParam = (email != null && !email.isBlank()) ? email.trim() : "unknown";

        String authUrl = "https://www.dropbox.com/oauth2/authorize?" +
                "client_id=" + dropboxClientId +
                "&redirect_uri=" + dropboxRedirectUri +
                "&response_type=code" +
                "&token_access_type=offline" +
                "&scope=files.content.read%20files.content.write" +
                "&state=" + stateParam;

        return ResponseEntity.ok(authUrl);
    }

    // --- DROPBOX OAUTH CALLBACK ---
    @GetMapping("/oauth/dropbox/callback")
    public ResponseEntity<Void> handleDropboxCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        String tokenEndpoint = "https://api.dropboxapi.com/oauth2/token";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", dropboxClientId);
        requestBody.add("client_secret", dropboxClientSecret);
        requestBody.add("code", code);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("redirect_uri", dropboxRedirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("refresh_token")) {
                String refreshToken = (String) responseBody.get("refresh_token");
                String userEmail = (state != null && !state.isBlank() && !state.equals("unknown")) ? state : "dropbox_" + System.currentTimeMillis() + "@example.com";

                Optional<Contributor> existingNode = contributorRepository.findByEmailAndCloudProvider(userEmail, "DROPBOX");
                List<Contributor> existingUserNodes = contributorRepository.findAllByEmail(userEmail);
                String existingPassword = !existingUserNodes.isEmpty() ? existingUserNodes.get(0).getPassword() : null;

                Contributor contributor;
                boolean shouldSendWelcomeEmail = false;

                if (existingNode.isPresent()) {
                    contributor = existingNode.get();
                    contributor.setRefreshToken(refreshToken);
                    if (existingPassword != null) contributor.setPassword(existingPassword);
                    if ("PENDING_OAUTH".equalsIgnoreCase(contributor.getStatus()) || contributor.getStatus() == null) {
                        contributor.setStatus("INACTIVE");
                        contributor.setAutoActivateAt(LocalDateTime.now().plusDays(4));
                        shouldSendWelcomeEmail = true;
                    }
                    log.info("[OAuthCallback] OAuth completed and Dropbox node updated for {}", userEmail);
                } else {
                    contributor = Contributor.builder()
                            .name(userEmail)
                            .email(userEmail)
                            .cloudProvider("DROPBOX")
                            .refreshToken(refreshToken)
                            .password(existingPassword)
                            .pledgedStorageBytes(5L * 1024 * 1024 * 1024)
                            .status("INACTIVE")
                            .tamperFlags(0)
                            .createdAt(LocalDateTime.now())
                            .autoActivateAt(LocalDateTime.now().plusDays(4))
                            .build();
                    shouldSendWelcomeEmail = true;
                    log.info("[OAuthCallback] OAuth completed and new Dropbox node created for {}", userEmail);
                }

                Contributor saved = contributorRepository.save(contributor);

                if (shouldSendWelcomeEmail) {
                    eventPublisher.publishEvent(EmailEvent.builder()
                            .source(this)
                            .eventType(EmailEvent.EventType.CONTRIBUTOR_REGISTERED)
                            .recipientEmail(saved.getEmail())
                            .recipientName(saved.getName() != null ? saved.getName() : saved.getEmail())
                            .cloudProvider(saved.getCloudProvider())
                            .pledgedBytes(saved.getPledgedStorageBytes())
                            .build());
                }

                String redirectUrl = frontendBaseUrl + "/contributor/register?status=success&provider=Dropbox";
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            } else {
                String redirectUrl = frontendBaseUrl + "/contributor/register?status=error&message=Failed%20to%20retrieve%20Dropbox%20refresh%20token";
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            }

        } catch (Exception e) {
            log.error("[OAuthCallback] Dropbox callback error: {}", e.getMessage(), e);
            String redirectUrl = frontendBaseUrl + "/contributor/register?status=error&message=" + e.getMessage();
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }
    }
}