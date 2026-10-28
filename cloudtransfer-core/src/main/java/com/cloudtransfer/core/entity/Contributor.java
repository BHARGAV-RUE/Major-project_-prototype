package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.UUID;

@Entity
@Table(name = "contributor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String email;

    @Column(name = "cloud_provider")
    private String cloudProvider; // e.g., GOOGLE_DRIVE or DROPBOX

    @Column(name = "password")
    private String password;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    @Column(name = "pledged_storage_bytes")
    private Long pledgedStorageBytes;

    @Column(name = "used_storage_bytes")
    private Long usedStorageBytes;

    private String status; // INACTIVE, ACTIVE, APPROVED, PENDING_TERMINATION, TERMINATED

    @Builder.Default
    @Column(name = "tamper_flags")
    private Integer tamperFlags = 0; // Tracks tampering warnings (1 to 3+)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "auto_activate_at")
    private LocalDateTime autoActivateAt;

    @Column(name = "scheduled_termination_at")
    private LocalDateTime scheduledTerminationAt; // Set when flags > 3 (4 business days grace period)

    @Column(name = "forfeit_requested_at")
    private LocalDateTime forfeitRequestedAt;

    public String getGoogleDriveRefreshToken() {
        return "GOOGLE_DRIVE".equalsIgnoreCase(cloudProvider) ? refreshToken : null;
    }

    public String getDropboxRefreshToken() {
        return "DROPBOX".equalsIgnoreCase(cloudProvider) ? refreshToken : null;
    }

    public String getStorageProvider() {
        return cloudProvider;
    }

    public String getProviderType() {
        return cloudProvider;
    }

    public void setStorageProvider(String provider) {
        this.cloudProvider = provider;
    }

    public void setProviderType(String provider) {
        this.cloudProvider = provider;
    }

    public Temporal getForfeitRequestedAt() {
        return forfeitRequestedAt;
    }

    public static class ContributorBuilder {
        public ContributorBuilder providerType(String provider) {
            this.cloudProvider = provider;
            return this;
        }

        public ContributorBuilder storageProvaider(String provider) {
            this.cloudProvider = provider;
            return this;
        }
    }
}