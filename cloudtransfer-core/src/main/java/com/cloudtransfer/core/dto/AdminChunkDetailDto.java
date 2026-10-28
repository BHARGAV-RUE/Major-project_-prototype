package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminChunkDetailDto {
    private UUID id;
    private Integer chunkIndex;
    private Long chunkSizeBytes;
    private String sha256Hash;
    private String status;

    private UUID primaryContributorId;
    private String primaryContributorEmail;
    private String primaryProvider;

    private UUID replicaContributorId;
    private String replicaContributorEmail;
    private String replicaProvider;
}