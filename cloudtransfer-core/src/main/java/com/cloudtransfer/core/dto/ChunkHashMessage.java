package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkHashMessage {
    private UUID sessionId;
    private Integer chunkIndex;
    private String chunkHash;
}