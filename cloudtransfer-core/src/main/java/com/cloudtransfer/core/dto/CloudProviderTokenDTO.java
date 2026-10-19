package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudProviderTokenDTO implements Serializable {

    private String providerName; // e.g., "GOOGLE_DRIVE" or "DROPBOX"
    private String volunteerEmail; // The email of the person donating storage
    private String refreshToken; // The actual OAuth token from the database

}