package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartTransferRequest {
    private String senderEmail;
    private String senderName;
    private List<String> recipientEmails;
    private String subject;
    private String message;
    private String filename;
    private Long totalSizeBytes;
    private Integer expiryDays;
}