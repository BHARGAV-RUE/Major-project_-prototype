package com.bhargav.cloudtransfer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "contributors")
@Getter
@Setter
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    private String folderId;

    @Column(length = 1000)
    private String refreshToken; // Stores the long-lived OAuth refresh token

    private String contributorEmail;

    private boolean active = true;
    @Column(name = "total_capacity_bytes")
    private Long totalCapacityBytes = 0L;

    @Column(name = "used_capacity_bytes")
    private Long usedCapacityBytes = 0L;

    @Column(name = "allocated_capacity_bytes")
    private Long allocatedCapacityBytes;

    public Long getAllocatedCapacityBytes() {
        return allocatedCapacityBytes;
    }

    public void setAllocatedCapacityBytes(Long allocatedCapacityBytes) {
        this.allocatedCapacityBytes = allocatedCapacityBytes;
    }

    // Helper method
    public Long getAvailableSpace() {
        if (totalCapacityBytes == null || usedCapacityBytes == null) {
            return 0L;
        }
        return totalCapacityBytes - usedCapacityBytes;
    }

    private LocalDateTime createdAt = LocalDateTime.now();
}