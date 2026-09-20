package com.bhargav.cloudtransfer.controller;

import com.bhargav.cloudtransfer.entity.Transfer;
import com.bhargav.cloudtransfer.service.TransferManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferManagerService transferManagerService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderEmail") String senderEmail,
            @RequestParam("expiryMinutes") int expiryMinutes) {
        try {
            String result = transferManagerService.initiateTransfer(file, senderEmail, expiryMinutes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{transferId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID transferId,
            @RequestParam("key") String masterKey) {
        try {
            // Get original file metadata
            Transfer transfer = transferManagerService.getTransferDetails(transferId);

            // Download and decrypt file bytes
            byte[] fileData = transferManagerService.downloadTransfer(transferId, masterKey);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + transfer.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileData.length)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}