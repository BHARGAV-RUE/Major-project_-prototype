package com.cloudtransfer.core.dto;

import java.io.Serializable;
import java.util.UUID;

public class ChunkHashMessage implements Serializable {

    private UUID sessionId;
    private Integer chunkIndex;
    private String chunkHash;
    private String cloudFileId;
    private String providerName;

    public ChunkHashMessage() {}

    public ChunkHashMessage(UUID sessionId, Integer chunkIndex, String chunkHash, String cloudFileId, String providerName) {
        this.sessionId = sessionId;
        this.chunkIndex = chunkIndex;
        this.chunkHash = chunkHash;
        this.cloudFileId = cloudFileId;
        this.providerName = providerName;
    }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getChunkHash() { return chunkHash; }
    public void setChunkHash(String chunkHash) { this.chunkHash = chunkHash; }

    public String getCloudFileId() { return cloudFileId; }
    public void setCloudFileId(String cloudFileId) { this.cloudFileId = cloudFileId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
}