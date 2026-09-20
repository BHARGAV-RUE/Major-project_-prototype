package com.bhargav.cloudtransfer.controller;

import com.bhargav.cloudtransfer.entity.SystemConfig;
import com.bhargav.cloudtransfer.repository.SystemConfigRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository systemConfigRepository;

    @PostMapping("/upload-json")
    public ResponseEntity<String> uploadCredentialsJson(@RequestParam("file") MultipartFile file) {
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(file.getInputStream())
            );

            GoogleClientSecrets.Details details = clientSecrets.getDetails();
            if (details == null) {
                return ResponseEntity.badRequest().body("Invalid JSON structure: missing client details.");
            }

            saveOrUpdate("GOOGLE_CLIENT_ID", details.getClientId());
            saveOrUpdate("GOOGLE_CLIENT_SECRET", details.getClientSecret());
            saveOrUpdate("GOOGLE_REDIRECT_URI", "http://localhost:8080/api/auth/google/callback");

            return ResponseEntity.ok("Success! Client ID, Secret, and Redirect URI extracted and stored in database automatically.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to parse credentials JSON: " + e.getMessage());
        }
    }

    private void saveOrUpdate(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key).orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }
}