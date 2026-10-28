package com.cloudtransfer.stream.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudProviderTokenDTO {

    private String providerName; // e.g., "GOOGLE_DRIVE" or "DROPBOX"
    private String volunteerEmail; // The email of the person donating storage
    private String refreshToken; // The actual OAuth token from the database
    private int chunkIndex;
    private boolean replica;

    // Multi-cloud primary & replica assignment fields
    private String primaryVolunteerEmail;
    private String primaryProvider;
    private String primaryRefreshToken;
    private String replicaVolunteerEmail;
    private String replicaProvider;
    private String replicaRefreshToken;

    /**
     * Legacy 3-parameter constructor for backward compatibility
     */
    public CloudProviderTokenDTO(String providerName, String volunteerEmail, String refreshToken) {
        this.providerName = providerName;
        this.volunteerEmail = volunteerEmail;
        this.refreshToken = refreshToken;
        this.chunkIndex = 0;
        this.replica = false;
    }

    /**
     * Alias getters for providerType compatibility
     */
    public String getProviderType() {
        if (providerName != null && !providerName.isBlank()) {
            return providerName;
        }
        return primaryProvider;
    }

    public void setProviderType(String providerType) {
        this.providerName = providerType;
        if (this.primaryProvider == null) {
            this.primaryProvider = providerType;
        }
    }

    public String getVolunteerEmail() {
        if (volunteerEmail != null && !volunteerEmail.isBlank()) {
            return volunteerEmail;
        }
        return primaryVolunteerEmail;
    }

    public String getRefreshToken() {
        if (refreshToken != null && !refreshToken.isBlank()) {
            return refreshToken;
        }
        return primaryRefreshToken;
    }

    public boolean isReplica() {
        return replica;
    }

    public void setReplica(boolean replica) {
        this.replica = replica;
    }
}