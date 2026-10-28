package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfer_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String senderEmail;
    private String senderName;
    private String subject;

    @Column(length = 2000)
    private String message;

    private String originalFilename;
    private Long totalSizeBytes;
    private Integer totalChunks;
    private Integer expiryDays;
    private LocalDateTime createdAt;
    private LocalDateTime expiryTimestamp;

    private String status; // CREATED, IN_PROGRESS, COMPLETED, PURGED
    private String merkleRoot;

    @OneToMany(mappedBy = "transferSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransferRecipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileChunk> chunks = new ArrayList<>();
}