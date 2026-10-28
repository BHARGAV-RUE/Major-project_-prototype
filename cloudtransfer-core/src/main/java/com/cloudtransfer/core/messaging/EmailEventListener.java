package com.cloudtransfer.core.messaging;

import com.cloudtransfer.core.event.EmailEvent;
import com.cloudtransfer.core.service.email.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailEventListener {

    private final ContributorRegistrationEmailService registrationEmailService;
    private final ContributorForfeitEmailService forfeitEmailService;
    private final FileTransferEmailService fileTransferEmailService;
    private final TamperWarningEmailService tamperWarningEmailService;
    private final ContributorTerminationEmailService terminationEmailService;

    @Async
    @EventListener
    public void handleEmailEvent(EmailEvent event) {
        if (event == null || event.getRecipientEmail() == null) {
            log.warn("[EmailEventListener] Received null or invalid email event.");
            return;
        }

        log.info("[EmailEventListener] Processing event [{}] for recipient: {}", event.getEventType(), event.getRecipientEmail());

        try {
            switch (event.getEventType()) {
                case CONTRIBUTOR_REGISTERED -> handleContributorRegistered(event);
                case CONTRIBUTOR_FORFEITED -> handleContributorForfeited(event);
                case TAMPER_WARNING -> handleTamperWarning(event);
                case TERMINATION_NOTICE_SCHEDULED -> handleTerminationNoticeScheduled(event);
                case IMMEDIATE_TERMINATION -> handleImmediateTermination(event);
                case TRANSFER_COMPLETE_RECIPIENT -> handleTransferCompleteRecipient(event);
                case TRANSFER_COMPLETE_SENDER -> handleTransferCompleteSender(event);
                default -> log.warn("[EmailEventListener] Unhandled email event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("[EmailEventListener] Error processing email event [{}] for {}: {}",
                    event.getEventType(), event.getRecipientEmail(), e.getMessage(), e);
        }
    }

    private void handleContributorRegistered(EmailEvent event) {
        registrationEmailService.sendRegistrationConfirmation(
                event.getRecipientEmail(),
                event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientEmail(),
                event.getCloudProvider() != null ? event.getCloudProvider() : "GOOGLE_DRIVE",
                event.getPledgedBytes()
        );
    }

    private void handleContributorForfeited(EmailEvent event) {
        forfeitEmailService.sendForfeitNotification(
                event.getRecipientEmail(),
                event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientEmail(),
                event.getCloudProvider() != null ? event.getCloudProvider() : "N/A"
        );
    }

    private void handleTamperWarning(EmailEvent event) {
        tamperWarningEmailService.sendTamperWarning(
                event.getRecipientEmail(),
                event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientEmail(),
                event.getCloudProvider() != null ? event.getCloudProvider() : "N/A",
                event.getCurrentFlags() != null ? event.getCurrentFlags() : 1
        );
    }

    private void handleTerminationNoticeScheduled(EmailEvent event) {
        terminationEmailService.sendScheduledTerminationNotice(
                event.getRecipientEmail(),
                event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientEmail(),
                event.getCloudProvider() != null ? event.getCloudProvider() : "N/A",
                event.getTerminationDate()
        );
    }

    private void handleImmediateTermination(EmailEvent event) {
        terminationEmailService.sendImmediateTerminationNotice(
                event.getRecipientEmail(),
                event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientEmail(),
                event.getCloudProvider() != null ? event.getCloudProvider() : "N/A",
                event.getMessage() != null ? event.getMessage() : "Manual Admin Purge due to security policy violations."
        );
    }

    private void handleTransferCompleteRecipient(EmailEvent event) {
        fileTransferEmailService.sendFileDownloadEmailToRecipient(
                event.getRecipientEmail(),
                event.getSenderName(),
                event.getSenderEmail(),
                event.getOriginalFilename(),
                event.getTotalSizeBytes(),
                event.getSubject(),
                event.getMessage(),
                event.getActionUrl(),
                event.getMerkleRoot()
        );
    }

    private void handleTransferCompleteSender(EmailEvent event) {
        fileTransferEmailService.sendTransferSentConfirmationToSender(
                event.getRecipientEmail(),
                event.getSenderName(),
                event.getRecipientEmails(),
                event.getOriginalFilename(),
                event.getTotalSizeBytes(),
                event.getMerkleRoot()
        );
    }
}