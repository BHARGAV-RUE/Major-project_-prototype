package com.bhargav.cloudtransfer.service;

import com.bhargav.cloudtransfer.repository.SystemConfigRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final SystemConfigRepository configRepository;

    private GoogleAuthorizationCodeFlow getFlow() throws Exception {
        String clientId = configRepository.findByConfigKey("GOOGLE_CLIENT_ID")
                .orElseThrow(() -> new RuntimeException("Google Client ID not found in database! Upload your JSON config first.")).getConfigValue();

        String clientSecret = configRepository.findByConfigKey("GOOGLE_CLIENT_SECRET")
                .orElseThrow(() -> new RuntimeException("Google Client Secret not found in database! Upload your JSON config first.")).getConfigValue();

        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(details);

        return new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Arrays.asList(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/userinfo.email"
                )
        )
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    private String getRedirectUri() {
        return configRepository.findByConfigKey("GOOGLE_REDIRECT_URI")
                .map(config -> config.getConfigValue())
                .orElse("http://localhost:8080/api/auth/google/callback");
    }

    public String getAuthorizationUrl() throws Exception {
        return getFlow().newAuthorizationUrl().setRedirectUri(getRedirectUri()).build();
    }

    public GoogleTokenResponse exchangeCodeForTokens(String code) throws Exception {
        return getFlow().newTokenRequest(code).setRedirectUri(getRedirectUri()).execute();
    }
}