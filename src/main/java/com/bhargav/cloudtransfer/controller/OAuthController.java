package com.bhargav.cloudtransfer.controller;

import com.bhargav.cloudtransfer.entity.Contributor;
import com.bhargav.cloudtransfer.entity.ProviderType;
import com.bhargav.cloudtransfer.repository.ContributorRepository;
import com.bhargav.cloudtransfer.service.GoogleDriveProviderService;
import com.bhargav.cloudtransfer.service.GoogleOAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class OAuthController {

    private final GoogleOAuthService oAuthService;
    private final GoogleDriveProviderService googleDriveProviderService;
    private final ContributorRepository contributorRepository;

    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        try {
            String url = oAuthService.getAuthorizationUrl();
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate login URL: " + e.getMessage());
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam("code") String code) {
        try {
            GoogleTokenResponse response = oAuthService.exchangeCodeForTokens(code);
            String refreshToken = response.getRefreshToken();

            if (refreshToken == null) {
                return ResponseEntity.badRequest().body("Failed to retrieve refresh token. Please re-authenticate.");
            }

            String email = "unknown-contributor@domain.com";
            if (response.getIdToken() != null) {
                GoogleIdToken idToken = GoogleIdToken.parse(
                        com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                        response.getIdToken()
                );
                email = idToken.getPayload().getEmail();
            }

            String folderId = googleDriveProviderService.getOrCreateAppFolder(refreshToken);
            long availableStorage = googleDriveProviderService.checkAvailableStorage(refreshToken);

            Contributor contributor = new Contributor();
            contributor.setContributorEmail(email);
            contributor.setRefreshToken(refreshToken);
            contributor.setFolderId(folderId);
            contributor.setProviderType(ProviderType.GOOGLE_DRIVE);
            contributor.setTotalCapacityBytes(availableStorage);
            contributor.setAllocatedCapacityBytes(availableStorage);
            contributor.setUsedCapacityBytes(0L);
            contributor.setActive(true);

            contributorRepository.save(contributor);

            long storageGB = availableStorage == Long.MAX_VALUE ? 0 : availableStorage / (1024 * 1024 * 1024);
            return ResponseEntity.ok("Successfully onboarded " + email + "! Added as a storage contributor with " + storageGB + " GB capacity.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("OAuth Onboarding failed: " + e.getMessage());
        }
    }

    @PutMapping("/update-limit")
    public ResponseEntity<String> updateContributorLimit(
            @RequestParam String email,
            @RequestParam Long allocatedCapacityMB) {

        return contributorRepository.findByContributorEmail(email).map(contributor -> {
            // Convert MB to Bytes (1 MB = 1024 * 1024 Bytes)
            long bytes = allocatedCapacityMB * 1024 * 1024;
            contributor.setAllocatedCapacityBytes(bytes);
            contributorRepository.save(contributor);
            return ResponseEntity.ok("Successfully updated storage limit for " + email + " to " + allocatedCapacityMB + " MB.");
        }).orElse(ResponseEntity.badRequest().body("Contributor not found."));
    }
}