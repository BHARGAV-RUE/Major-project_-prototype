package com.cloudtransfer.core.event;

import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class EmailEvent extends ApplicationEvent {

    public enum EventType {
        TRANSFER_COMPLETE_RECIPIENT,
        TRANSFER_COMPLETE_SENDER,
        CONTRIBUTOR_REGISTERED,
        CONTRIBUTOR_FORFEITED,
        TAMPER_WARNING,
        TERMINATION_NOTICE_SCHEDULED,
        IMMEDIATE_TERMINATION
    }

    private final EventType eventType;
    private final String recipientEmail;
    private final String recipientName;
    private final String senderEmail;
    private final String senderName;
    private final String subject;
    private final String message;
    private final String actionUrl;
    private final String cloudProvider;
    private final Long pledgedBytes;
    private final Integer currentFlags;
    private final LocalDateTime terminationDate;
    private final String originalFilename;
    private final Long totalSizeBytes;
    private final String merkleRoot;
    private final List<String> recipientEmails;

    @Builder
    public EmailEvent(
            Object source,
            EventType eventType,
            String recipientEmail,
            String recipientName,
            String senderEmail,
            String senderName,
            String subject,
            String message,
            String actionUrl,
            String cloudProvider,
            Long pledgedBytes,
            Integer currentFlags,
            LocalDateTime terminationDate,
            String originalFilename,
            Long totalSizeBytes,
            String merkleRoot,
            List<String> recipientEmails) {
        super(source != null ? source : new Object());
        this.eventType = eventType;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.subject = subject;
        this.message = message;
        this.actionUrl = actionUrl;
        this.cloudProvider = cloudProvider;
        this.pledgedBytes = pledgedBytes;
        this.currentFlags = currentFlags;
        this.terminationDate = terminationDate;
        this.originalFilename = originalFilename;
        this.totalSizeBytes = totalSizeBytes;
        this.merkleRoot = merkleRoot;
        this.recipientEmails = recipientEmails;
    }
}