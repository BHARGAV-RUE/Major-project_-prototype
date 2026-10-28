package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "transfer_recipients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String recipientEmail;
    private Boolean notified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_session_id")
    private TransferSession transferSession;
}