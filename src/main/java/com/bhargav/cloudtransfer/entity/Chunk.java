package com.bhargav.cloudtransfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "chunks")
@Getter @Setter @NoArgsConstructor
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    private Integer chunkIndex;
    private Long sizeBytes;

    // Crucial for your tamper-evident integrity verification
    private String checksumHash;

    @Enumerated(EnumType.STRING)
    private ChunkStatus status;
    private String encryptionNonce;
}