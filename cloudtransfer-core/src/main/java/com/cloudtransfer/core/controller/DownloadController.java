package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.entity.TransferSession;
import com.cloudtransfer.core.repository.FileChunkRepository;
import com.cloudtransfer.core.repository.TransferSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final TransferSessionRepository transferSessionRepository;
    private final FileChunkRepository fileChunkRepository;

    @GetMapping("/download/{sessionId}")
    public ResponseEntity<String> getDownloadManifest(@PathVariable UUID sessionId) {
        TransferSession session = transferSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Transfer session not found: " + sessionId));

        // Using your exact existing repository method here
        List<FileChunk> chunks = fileChunkRepository.findBySessionIdOrderByChunkIndexAsc(sessionId);

        log.info("[Brain] Download manifest requested for file: {} containing {} chunks",
                session.getOriginalFilename(), chunks.size());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"sessionId\": \"" + sessionId + "\", \"filename\": \"" + session.getOriginalFilename() + "\", \"totalChunks\": " + chunks.size() + "}");
    }
}