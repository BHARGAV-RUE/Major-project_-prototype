package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkAssignmentDto {
    private Integer chunkIndex;
    private UUID primaryContributorId;
    private String primaryProvider;
    private String primaryRefreshToken;

    private UUID replicaContributorId;
    private String replicaProvider;
    private String replicaRefreshToken;
}