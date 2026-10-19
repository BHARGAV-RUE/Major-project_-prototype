package com.cloudtransfer.stream.dto; // Use your exact package name

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProcessCommandMessage implements Serializable {

    private String fileId; // The unique ID of the file in your system
    private String fileName;
    private String localFilePath; // Where Muscle can find the temporary file to chunk it

    // This is the bridge! Brain passes the DB tokens to Muscle here:
    private List<CloudProviderTokenDTO> volunteerTokens;

}