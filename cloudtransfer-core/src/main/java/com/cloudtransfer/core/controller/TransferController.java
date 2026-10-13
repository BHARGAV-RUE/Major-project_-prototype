package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.dto.BlueprintResponseDto;
import com.cloudtransfer.core.dto.UploadRequestDto;
import com.cloudtransfer.core.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final DistributionService distributionService;

    @PostMapping("/blueprint")
    public ResponseEntity<BlueprintResponseDto> requestUploadBlueprint(@RequestBody UploadRequestDto request) {
        BlueprintResponseDto blueprint = distributionService.createBlueprint(
                request.getSenderEmail(),
                request.getFilename(),
                request.getTotalSizeBytes(),
                request.getExpiryDays() != null ? request.getExpiryDays() : 5 // Default to 5 days
        );

        return ResponseEntity.ok(blueprint);
    }
}