package com.cloudtransfer.core.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ContributorTerminationEmailService extends BaseEmailService {

    public ContributorTerminationEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    // --- 4-BUSINESS-DAY TERMINATION NOTICE (FLAGS > 3) ---
    public void sendScheduledTerminationNotice(String toEmail, String name, String cloudProvider, LocalDateTime terminationDate) {
        String formattedDate = terminationDate != null
                ? terminationDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"))
                : "4 Business Days";

        String subject = "🚨 FINAL TERMINATION NOTICE: Account Scheduled for Purge in 4 Business Days";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #ef4444; margin-bottom: 8px;">🚨 Account Scheduled for Termination</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>You have exceeded the maximum allowed warning threshold (<strong>3 warning flags reached</strong>) due to repeated data tampering or chunk deletion on your <strong>%s</strong> storage vault.</p>
                
                <div style="background-color: #450a0a; border: 1px solid #991b1b; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0; color: #fca5a5;"><strong>Violation:</strong> Repeated Data Tampering & Hash Mismatch</p>
                    <p style="margin: 4px 0; color: #fca5a5;"><strong>Scheduled Purge Date:</strong> %s</p>
                </div>
                
                <p style="font-size: 13px; color: #94a3b8;">
                    As per network rules, your contributor account and node connection will be <strong>permanently purged within 4 business days</strong>. Remaining chunk assignments are being rerouted to replica nodes.
                </p>
            </div>
            """.formatted(name, cloudProvider, formattedDate);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    // --- IMMEDIATE PURGE / BAN CONFIRMATION ---
    public void sendImmediateTerminationNotice(String toEmail, String name, String cloudProvider, String reason) {
        String subject = "🚨 CloudTransfer - Contributor Node Purged";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #ef4444; margin-bottom: 8px;">Node Terminated & Banned</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your <strong>%s</strong> contributor node has been officially purged and banned from the CloudTransfer network.</p>
                <div style="background-color: #450a0a; border: 1px solid #991b1b; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0; color: #fca5a5;"><strong>Reason:</strong> %s</p>
                </div>
            </div>
            """.formatted(name, cloudProvider, reason);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}