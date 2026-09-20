package com.bhargav.cloudtransfer.dto;

import com.bhargav.cloudtransfer.entity.ProviderType;
import lombok.Data;

@Data
public class ContributorRegistrationRequest {
    private ProviderType providerType;
    private String folderId;
    private String refreshToken;
    private String contributorEmail;

    // Add this new field to capture the storage limit from Postman
    private Long totalCapacityBytes;
}