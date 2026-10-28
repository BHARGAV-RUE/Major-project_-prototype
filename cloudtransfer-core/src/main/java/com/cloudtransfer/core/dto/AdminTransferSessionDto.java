package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTransferSessionDto {
    private UUID id;
    private String senderEmail;
    private String senderName;
    private String subject;
    private String message;
    private String originalFilename;
    private Long totalSizeBytes;
    private Integer totalChunks;
    private Integer expiryDays;
    private LocalDateTime createdAt;
    private LocalDateTime expiryTimestamp;
    private String status;
    private String merkleRoot;
    private List<String> recipientEmails;
    private List<AdminChunkDetailDto> chunks;
    private String blockchainStatus; // "ANCHORED" or "ON_CHAIN_MISMATCH"
}