package com.cloudtransfer.core.controller;

import com.cloudtransfer.core.dto.BlueprintResponseDto;
import com.cloudtransfer.core.dto.UploadRequestDto;
import com.cloudtransfer.core.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import com.cloudtransfer.core.messaging.FileProducerService;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final DistributionService distributionService;
    private final FileProducerService fileProducerService;

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
    @GetMapping("/test-bridge")
    public String testRabbitMQBridge() {
        // Using your exact DTO fields
        CloudProviderTokenDTO googleToken = new CloudProviderTokenDTO("GOOGLE_DRIVE", "volunteer1@gmail.com", "1//0g_fake_google_token...");
        CloudProviderTokenDTO dropboxToken = new CloudProviderTokenDTO("DROPBOX", "volunteer2@dropbox.com", "sl.fake_dropbox_token...");

        // Wrap in a standard java.util.ArrayList to prevent the SecurityException in Muscle
        List<CloudProviderTokenDTO> tokens = new java.util.ArrayList<>(Arrays.asList(googleToken, dropboxToken));

        // Fire the message from Brain to Muscle
        fileProducerService.sendFileForProcessing("file-001", "BTech_Project_Demo.mp4", "C:/temp/BTech_Project_Demo.mp4", tokens);

        return "Message fired successfully! Check the Muscle console.";
    }

}