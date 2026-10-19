package com.cloudtransfer.stream.controller;

import com.cloudtransfer.stream.service.FileChunkProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamController {

    private final FileChunkProcessorService fileChunkProcessorService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Muscle Stream Engine is operational.");
    }
}