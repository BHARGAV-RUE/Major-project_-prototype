package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TamperAlertDto {
    private String alertId;
    private UUID sessionId;
    private Integer chunkIndex;
    private UUID contributorId;
    private String contributorEmail;
    private String cloudProvider;
    private String tamperType; // "NEW_REGISTRATION_REQUEST", "DELETED_FILE_OR_TAMPERED", "WARNING_ISSUED", "PENDING_TERMINATION"
    private Integer tamperFlags; // Current flag count (1, 2, 3, 4+)
    private LocalDateTime timestamp;
    private LocalDateTime scheduledTerminationAt;
    private String status;
}