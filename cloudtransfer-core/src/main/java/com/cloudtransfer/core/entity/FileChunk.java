package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "file_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TransferSession session;

    private Integer chunkIndex;
    private String chunkHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contributor_id")
    private Contributor primaryContributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replica_contributor_id")
    private Contributor replicaContributor;

    private String cloudFileId;
    private String providerName;
}