package com.cloudtransfer.stream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudStorageUploaderService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${google.oauth.client-secret}")
    private String googleClientSecret;

    @Value("${dropbox.oauth.client-id}")
    private String dropboxClientId;

    @Value("${dropbox.oauth.client-secret}")
    private String dropboxClientSecret;

    /**
     * Refreshes a Google Drive OAuth refresh token to get a short-lived access token.
     */
    public String refreshGoogleAccessToken(String refreshToken) {
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Google Drive token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Refreshes a Dropbox OAuth refresh token to get a short-lived access token.
     */
    public String refreshDropboxAccessToken(String refreshToken) {
        String url = "https://api.dropbox.com/oauth2/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", dropboxClientId);
        body.add("client_secret", dropboxClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Dropbox token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Streams an encrypted chunk byte array to Google Drive.
     */
    public boolean uploadChunkToGoogleDrive(String accessToken, String chunkFileName, byte[] encryptedChunkData) {
        String uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=media";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> request = new HttpEntity<>(encryptedChunkData, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Google Drive chunk upload failed for {}: {}", chunkFileName, e.getMessage());
            return false;
        }
    }

    /**
     * Streams an encrypted chunk byte array to Dropbox.
     */
    public boolean uploadChunkToDropbox(String accessToken, String chunkFileName, byte[] encryptedChunkData) {
        String uploadUrl = "https://content.dropboxapi.com/2/files/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Dropbox-API-Arg", "{\"path\": \"/" + chunkFileName + "\",\"mode\": \"add\",\"autorename\": true,\"mute\": false}");

        HttpEntity<byte[]> request = new HttpEntity<>(encryptedChunkData, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Dropbox chunk upload failed for {}: {}", chunkFileName, e.getMessage());
            return false;
        }
    }
}