package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "transfer_sessions")
public class TransferSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String senderEmail;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private Long totalSizeBytes;

    private String merkleRoot;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}