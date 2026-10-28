package com.cloudtransfer.stream.controller;

import com.cloudtransfer.stream.dto.CloudProviderTokenDTO;
import com.cloudtransfer.stream.service.FileChunkProcessorService;
import com.cloudtransfer.stream.service.FileDownloadProcessorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/stream", "/api/v1/stream"})
@Slf4j
@RequiredArgsConstructor
public class StreamController {

    private final FileChunkProcessorService fileChunkProcessorService;
    private final FileDownloadProcessorService fileDownloadProcessorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/session/register")
    public ResponseEntity<Map<String, Object>> registerSession(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        Integer totalChunks = (Integer) payload.get("totalChunks");
        List<CloudProviderTokenDTO> tokens = objectMapper.convertValue(
                payload.get("tokens"),
                new TypeReference<List<CloudProviderTokenDTO>>() {}
        );

        log.info("[StreamController] Pre-registering session: {}, totalChunks: {}", sessionId, totalChunks);
        fileChunkProcessorService.registerSessionTokens(sessionId, totalChunks, tokens);

        return ResponseEntity.ok(Map.of("status", "REGISTERED", "sessionId", sessionId));
    }

    @GetMapping("/session/{sessionId}/hashes")
    public ResponseEntity<List<Map<String, Object>>> getSessionHashes(@PathVariable String sessionId) {
        log.info("[StreamController] Fetching buffered hashes for session: {}", sessionId);
        List<Map<String, Object>> hashes = fileChunkProcessorService.getSessionHashes(sessionId);
        return ResponseEntity.ok(hashes);
    }

    @PostMapping({"/upload", "/upload-chunk"})
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "chunkIndex", required = false, defaultValue = "0") Integer chunkIndex,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "tokens", required = false) String tokensJson) {

        log.info("[StreamController] Received upload request for session: {}, chunkIndex: {}", sessionId, chunkIndex);

        try {
            MultipartFile[] uploadFiles;
            if (files != null && files.length > 0) {
                uploadFiles = files;
            } else if (file != null) {
                uploadFiles = new MultipartFile[]{file};
            } else {
                throw new IllegalArgumentException("No file or files parameter provided in request.");
            }

            List<CloudProviderTokenDTO> tokenList = null;
            if (tokensJson != null && !tokensJson.isBlank()) {
                tokenList = objectMapper.readValue(tokensJson, new TypeReference<List<CloudProviderTokenDTO>>() {});
            }

            Map<String, Object> result = fileChunkProcessorService.processAndUploadMultipleChunks(
                    sessionId, chunkIndex, uploadFiles, tokenList
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[StreamController] Upload chunk error for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Chunk processing failed: " + e.getMessage(), e);
        }
    }

    @GetMapping({"/download-chunk", "/download"})
    public ResponseEntity<?> downloadChunk(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "chunkIndex", defaultValue = "0") Integer chunkIndex,
            @RequestParam(value = "filename", required = false) String filename) {

        log.info("[StreamController] Received download request for session: {}, chunkIndex: {}", sessionId, chunkIndex);

        try {
            byte[] decryptedBytes = fileDownloadProcessorService.downloadAndDecryptChunk(sessionId, chunkIndex);

            if (decryptedBytes == null || decryptedBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "Chunk byte payload is empty or unavailable."));
            }

            String downloadName = (filename != null && !filename.isBlank()) ? filename : "chunk_" + chunkIndex;
            ByteArrayResource resource = new ByteArrayResource(decryptedBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentLength(decryptedBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("[StreamController] Failed to process chunk download for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Chunk download failed"));
        }
    }
}