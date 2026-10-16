package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.service.BlockchainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    private final BlockchainService blockchainService;

    public AuditController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/verify/{sessionId}")
    public ResponseEntity<String> verifyFileIntegrity(
            @PathVariable String sessionId,
            @RequestParam String recomputedRoot) {

        logger.info("Audit requested for Session: {}", sessionId);

        boolean isValid = blockchainService.verifyIntegrityOnChain(sessionId, recomputedRoot);

        if (isValid) {
            return ResponseEntity.ok("SUCCESS: File integrity verified on Polygon Amoy. No tampering detected.");
        } else {
            return ResponseEntity.status(409).body("WARNING: File has been tampered with or root does not match the blockchain ledger.");
        }
    }
}