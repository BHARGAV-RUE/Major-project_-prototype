package com.cloudtransfer.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "contributors")
public class Contributor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String provider; // e.g., GOOGLE_DRIVE, DROPBOX

    @Column(nullable = false, length = 1000)
    private String refreshToken;

    @Column(nullable = false)
    private Long allocatedBytes;

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, DRAINING, BANNED
}