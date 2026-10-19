package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.dto.BlueprintResponseDto;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import com.cloudtransfer.core.dto.UploadRequestDto;
import com.cloudtransfer.core.messaging.FileProducerService;
import com.cloudtransfer.core.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final DistributionService distributionService;
    private final FileProducerService fileProducerService;

    @PostMapping("/blueprint")
    public ResponseEntity<?> requestUploadBlueprint(@RequestBody UploadRequestDto request) {
        try {
            BlueprintResponseDto blueprint = distributionService.createBlueprint(
                    request.getSenderEmail(),
                    request.getFilename(),
                    request.getTotalSizeBytes(),
                    request.getExpiryDays() != null ? request.getExpiryDays() : 5
            );
            return ResponseEntity.ok(blueprint);
        } catch (IllegalStateException e) {
            // Return custom capacity error message directly to user/frontend
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startDynamicTransfer(@RequestBody UploadRequestDto request) {
        try {
            // 1. Create session blueprint & enforce volunteer capacity checks
            BlueprintResponseDto blueprint = distributionService.createBlueprint(
                    request.getSenderEmail(),
                    request.getFilename(),
                    request.getTotalSizeBytes(),
                    request.getExpiryDays() != null ? request.getExpiryDays() : 5
            );

            // 2. Retrieve tokens for active contributors
            List<CloudProviderTokenDTO> tokens = distributionService.getAvailableVolunteerTokens();

            // 3. Command Muscle over RabbitMQ to start processing file
            fileProducerService.sendFileForProcessing(
                    blueprint.getSessionId().toString(),
                    request.getFilename(),
                    request.getFilePath(),
                    tokens
            );

            return ResponseEntity.ok("Transfer session " + blueprint.getSessionId() + " initialized for file: " + request.getFilename());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}