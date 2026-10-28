package com.cloudtransfer.stream.storage;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

@Component
@Slf4j
public class DropboxStorageProvider {

    @Value("${dropbox.oauth.client-id}")
    private String clientId;

    @Value("${dropbox.oauth.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchanges the volunteer's permanent refreshToken (from DB) for a short-lived accessToken at runtime.
     */
    private String fetchAccessTokenFromRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Volunteer refresh token cannot be empty.");
        }

        try {
            String tokenUrl = "https://api.dropbox.com/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                if (accessToken != null && !accessToken.isBlank()) {
                    log.info("[DropboxProvider] Successfully obtained runtime access token for volunteer node.");
                    return accessToken;
                }
            }
        } catch (Exception e) {
            log.error("[DropboxProvider] Failed to exchange refresh token for access token: {}", e.getMessage(), e);
            throw new RuntimeException("Dropbox token exchange failed: " + e.getMessage(), e);
        }

        throw new IllegalStateException("Could not obtain valid access token from Dropbox OAuth endpoint.");
    }

    private DbxClientV2 createClient(String refreshToken) {
        String accessToken = fetchAccessTokenFromRefreshToken(refreshToken);

        StandardHttpRequestor customRequestor = new DevStandardHttpRequestor(
                StandardHttpRequestor.Config.builder().build()
        );

        DbxRequestConfig config = DbxRequestConfig.newBuilder("CloudTransfer-App/1.0")
                .withHttpRequestor(customRequestor)
                .build();

        return new DbxClientV2(config, accessToken);
    }

    public String uploadChunkToContributor(String refreshToken, String sessionId, int chunkIndex, String filename, byte[] data) {
        log.info("[DropboxProvider] Uploading chunk #{} ({} bytes) for session {} to Dropbox", chunkIndex, data.length, sessionId);

        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            DbxClientV2 client = createClient(refreshToken);
            String dropboxPath = "/CloudTransfer_Vault/" + sessionId + "_chunk_" + chunkIndex + "_" + filename;

            var metadata = client.files().uploadBuilder(dropboxPath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream);

            log.info("[DropboxProvider] Successfully uploaded chunk to Dropbox. Path: {}", metadata.getPathLower());
            return metadata.getPathLower();
        } catch (Exception e) {
            log.error("[DropboxProvider] Dropbox chunk upload failed for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Dropbox upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Alias method for MultiCloudRouter compatibility.
     */
    public String uploadChunk(String sessionId, int chunkIndex, byte[] data, String filename, String refreshToken) {
        return uploadChunkToContributor(refreshToken, sessionId, chunkIndex, filename, data);
    }

    public byte[] downloadChunkFromContributor(String refreshToken, String sessionId, int chunkIndex) {
        log.info("[DropboxProvider] Downloading chunk #{} for session {} from Dropbox", chunkIndex, sessionId);
        try {
            DbxClientV2 client = createClient(refreshToken);

            String targetFolder = "/CloudTransfer_Vault";
            String searchPattern = sessionId + "_chunk_" + chunkIndex;

            String targetPath;
            try {
                var result = client.files().listFolderBuilder(targetFolder).start();
                targetPath = result.getEntries().stream()
                        .filter(metadata -> metadata.getName().contains(searchPattern))
                        .map(metadata -> metadata.getPathLower())
                        .findFirst()
                        .orElse(targetFolder + "/" + searchPattern);
            } catch (Exception e) {
                log.warn("[DropboxProvider] Could not list folder '{}', falling back to root listing or computed path", targetFolder);
                var rootResult = client.files().listFolderBuilder("").start();
                targetPath = rootResult.getEntries().stream()
                        .filter(metadata -> metadata.getName().contains(searchPattern))
                        .map(metadata -> metadata.getPathLower())
                        .findFirst()
                        .orElse(targetFolder + "/" + searchPattern);
            }

            log.info("[DropboxProvider] Downloading chunk from path: {}", targetPath);
            try (var outputStream = new java.io.ByteArrayOutputStream()) {
                client.files().downloadBuilder(targetPath).download(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("[DropboxProvider] Dropbox chunk download failed for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Dropbox download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Alias method for MultiCloudRouter download compatibility.
     */
    public byte[] downloadChunk(String refreshToken, String sessionId, int chunkIndex) {
        return downloadChunkFromContributor(refreshToken, sessionId, chunkIndex);
    }

    /**
     * Custom HttpRequestor that configures SSL socket factories directly on HttpsURLConnection for local dev
     */
    private static class DevStandardHttpRequestor extends StandardHttpRequestor {

        public DevStandardHttpRequestor(Config config) {
            super(config);
        }

        @Override
        protected void configure(HttpURLConnection conn) throws IOException {
            super.configure(conn);
            if (conn instanceof HttpsURLConnection httpsConn) {
                try {
                    httpsConn.setSSLSocketFactory(createUnsafeSslSocketFactory());
                    httpsConn.setHostnameVerifier((hostname, session) -> true);
                } catch (Exception e) {
                    log.error("[DropboxProvider] Error applying custom SSL socket factory to connection", e);
                }
            }
        }

        private SSLSocketFactory createUnsafeSslSocketFactory() {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                return sslContext.getSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize custom SSL Context", e);
            }
        }
    }
}