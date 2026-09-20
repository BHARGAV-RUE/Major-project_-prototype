package com.bhargav.cloudtransfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "download_tokens")
@Getter @Setter @NoArgsConstructor
public class DownloadToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    // We store a hashed version of the token for security, never the plain text
    private String tokenHash;

    private LocalDateTime expiresAt;

    private boolean isRevoked = false;
}