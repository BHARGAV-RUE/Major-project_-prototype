package com.bhargav.cloudtransfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "chunk_distributions")
@Getter @Setter @NoArgsConstructor
public class ChunkDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private Chunk chunk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id")
    private Contributor contributor;

    // The file ID Google Drive or Dropbox gives us
    private String remoteReferenceId;

    // For your replication vs erasure coding experiments
    private boolean isPrimary;
}