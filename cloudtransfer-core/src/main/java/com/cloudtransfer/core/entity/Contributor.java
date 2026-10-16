package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "contributors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String email;

    // "GOOGLE_DRIVE" or "DROPBOX"
    private String cloudProvider;

    // The offline access token used by Muscle to upload chunks quietly
    @Column(length = 500)
    private String refreshToken;

    // Space the contributor is willing to donate (in bytes)
    private Long maxStorageQuotaBytes;

    // "ACTIVE", "PENDING_DELETION", "OFFLINE"
    private String status;
}