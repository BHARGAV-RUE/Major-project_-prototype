package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.dto.CloudProviderTokenDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileProcessCommandMessage implements Serializable {

    private String fileId;
    private String sessionId;
    private String filename;
    private String filePath;
    private Integer chunkIndex;
    private String refreshToken;
    private String cloudProvider;
    private List<CloudProviderTokenDTO> tokens;

    // Constructor for FileProducerService (bulk processing)
    public FileProcessCommandMessage(String fileId, String filename, String filePath, List<CloudProviderTokenDTO> tokens) {
        this.fileId = fileId;
        this.sessionId = fileId;
        this.filename = filename;
        this.filePath = filePath;
        this.tokens = tokens;
    }

    // Constructor for TransferService (chunk-level processing)
    public FileProcessCommandMessage(String sessionId, String filename, String filePath, Integer chunkIndex, String refreshToken, String cloudProvider) {
        this.sessionId = sessionId;
        this.fileId = sessionId;
        this.filename = filename;
        this.filePath = filePath;
        this.chunkIndex = chunkIndex;
        this.refreshToken = refreshToken;
        this.cloudProvider = cloudProvider;
    }
}