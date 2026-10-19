package com.cloudtransfer.stream.dto;

import com.cloudtransfer.stream.dto.CloudProviderTokenDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProcessCommandMessage {
    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("tokens")
    private List<CloudProviderTokenDTO> tokens;
}