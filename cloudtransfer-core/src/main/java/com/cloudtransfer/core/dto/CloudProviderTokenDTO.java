package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
     * Alias getter/setter for providerType compatibility
     */
    public String getProviderType() {
        return providerName != null ? providerName : primaryProvider;
    }

    public void setProviderType(String providerType) {
        this.providerName = providerType;
        this.primaryProvider = providerType;
    }

    public String getVolunteerEmail() {
        return volunteerEmail != null ? volunteerEmail : primaryVolunteerEmail;
    }

    public String getRefreshToken() {
        return refreshToken != null ? refreshToken : primaryRefreshToken;
    }

    public boolean isReplica() {
        return replica;
    }

    public void setReplica(boolean replica) {
        this.replica = replica;
    }
}