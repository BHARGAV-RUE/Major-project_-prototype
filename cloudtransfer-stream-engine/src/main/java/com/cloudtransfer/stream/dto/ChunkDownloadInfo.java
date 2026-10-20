package com.cloudtransfer.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkDownloadInfo {
    private int chunkIndex;
    private String cloudFileId;
    private String providerName;
    private String expectedHash;
    private String refreshToken;
}