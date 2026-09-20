package com.bhargav.cloudtransfer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_configs")
@Data
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String configKey; // e.g., "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configValue;
}