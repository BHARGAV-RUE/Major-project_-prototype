package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "file_chunks")
public class FileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TransferSession session;

    @Column(nullable = false)
    private Integer chunkIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contributor_id")
    private Contributor primaryContributor;

    private String primaryRemoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replica_contributor_id")
    private Contributor replicaContributor;

    private String replicaRemoteId;

    private String chunkHash;
}