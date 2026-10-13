package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlueprintResponseDto {
    private UUID sessionId;
    private Long totalSizeBytes;
    private Integer totalChunks;
    private List<ChunkAssignmentDto> chunkAssignments;
}