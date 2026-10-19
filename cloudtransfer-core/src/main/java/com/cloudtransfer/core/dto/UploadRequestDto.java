package com.cloudtransfer.core.dto;

import lombok.Data;

@Data
public class UploadRequestDto {
    private String senderEmail;
    private String filename;
    private String filePath;
    private Long totalSizeBytes;
    private Integer expiryDays;
}