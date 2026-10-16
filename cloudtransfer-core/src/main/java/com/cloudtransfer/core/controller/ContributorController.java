package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contributors")
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorRepository contributorRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    /**
     * 1. Generates the Google Login URL for the frontend.
     * The user clicks this link to grant access to their Google Drive.
     */
    @GetMapping("/oauth/google/url")
    public ResponseEntity<String> getGoogleAuthUrl() {
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/drive.file" +
                "&access_type=offline" +
                "&prompt=consent"; // Forces prompt to ensure we get a refresh_token

        return ResponseEntity.ok(authUrl);
    }

    /**
     * 2. Google redirects the user back here with an Authorization Code.
     * We exchange this code for a Refresh Token and save it to the DB.
     */
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam("code") String code) {

        // Prepare the token exchange request
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
            // Send request to Google
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("refresh_token")) {
                String refreshToken = (String) responseBody.get("refresh_token");

                // Save the new contributor to the database
                Contributor contributor = Contributor.builder()
                        .email("contributor_" + System.currentTimeMillis() + "@example.com") // In production, fetch from Google Profile API
                        .cloudProvider("GOOGLE_DRIVE")
                        .refreshToken(refreshToken)
                        .maxStorageQuotaBytes(5L * 1024 * 1024 * 1024) // Default to 5 GB donation
                        .status("ACTIVE")
                        .build();

                contributorRepository.save(contributor);

                return ResponseEntity.ok("SUCCESS! Your Google Drive is now connected. Refresh Token saved to Database.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to retrieve refresh token. You might have already granted access without prompting consent.");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("OAuth Exchange Failed: " + e.getMessage());
        }
    }

    // --- DROPBOX OAUTH FIELDS ---
    @Value("${dropbox.oauth.client-id}")
    private String dropboxClientId;

    @Value("${dropbox.oauth.client-secret}")
    private String dropboxClientSecret;

    @Value("${dropbox.oauth.redirect-uri}")
    private String dropboxRedirectUri;

    /**
     * 3. Generates the Dropbox Login URL
     */
    @GetMapping("/oauth/dropbox/url")
    public ResponseEntity<String> getDropboxAuthUrl() {
        String authUrl = "https://www.dropbox.com/oauth2/authorize?" +
                "client_id=" + dropboxClientId +
                "&redirect_uri=" + dropboxRedirectUri +
                "&response_type=code" +
                "&token_access_type=offline" +
                "&scope=files.content.read%20files.content.write";

        return ResponseEntity.ok(authUrl);
    }

    /**
     * 4. Dropbox Callback Handler
     */
    @GetMapping("/oauth/dropbox/callback")
    public ResponseEntity<String> handleDropboxCallback(@RequestParam("code") String code) {

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

                Contributor contributor = Contributor.builder()
                        .email("dropbox_contributor_" + System.currentTimeMillis() + "@example.com")
                        .cloudProvider("DROPBOX")
                        .refreshToken(refreshToken)
                        .maxStorageQuotaBytes(2L * 1024 * 1024 * 1024) // Default 2 GB donation
                        .status("ACTIVE")
                        .build();

                contributorRepository.save(contributor);

                return ResponseEntity.ok("SUCCESS! Your Dropbox is now connected. Refresh Token saved to Database.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to retrieve refresh token from Dropbox.");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Dropbox OAuth Exchange Failed: " + e.getMessage());
        }
    }
}