package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "transfer_recipients")
public class TransferRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TransferSession session;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(unique = true, nullable = false)
    private String downloadToken;
}