package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "file_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer chunkIndex;
    private Long chunkSizeBytes;
    private String cloudFileId;
    private String sha256Hash;
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TransferSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contributor_id")
    private Contributor primaryContributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replica_contributor_id")
    private Contributor replicaContributor;

    // Explicit accessors for chunkIndex
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
}