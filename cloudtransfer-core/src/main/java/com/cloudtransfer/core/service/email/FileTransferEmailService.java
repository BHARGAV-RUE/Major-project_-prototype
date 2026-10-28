package com.cloudtransfer.core.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FileTransferEmailService extends BaseEmailService {

    public FileTransferEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    // --- RECIPIENT DOWNLOAD LINK EMAIL ---
    public void sendFileDownloadEmailToRecipient(
            String recipientEmail,
            String senderName,
            String senderEmail,
            String originalFilename,
            Long totalSizeBytes,
            String subjectHeader,
            String customMessage,
            String downloadUrl,
            String merkleRoot) {

        double sizeMb = totalSizeBytes != null ? (double) totalSizeBytes / (1024 * 1024) : 0.0;
        String emailSubject = "📦 File Received via CloudTransfer: " + originalFilename;

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #38bdf8; margin-bottom: 8px;">You Received a File Transfer!</h2>
                <p><strong>%s</strong> (%s) sent you a secure encrypted file.</p>
                
                <div style="background-color: #1e293b; border-left: 4px solid #38bdf8; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0;"><strong>File Name:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>File Size:</strong> %.2f MB</p>
                    <p style="margin: 4px 0;"><strong>Subject:</strong> %s</p>
                    %s
                </div>

                <div style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="background-color: #0284c7; color: #ffffff; padding: 14px 28px; text-decoration: none; font-weight: bold; font-size: 14px; border-radius: 10px; display: inline-block;">
                        📥 DOWNLOAD ENCRYPTED FILE
                    </a>
                </div>

                <div style="background-color: #022c22; border: 1px solid #059669; padding: 12px; border-radius: 8px; font-family: monospace; font-size: 11px; color: #6ee7b7; word-break: break-all;">
                    ⛓️ Polygon Merkle Root: %s
                </div>
            </div>
            """.formatted(
                senderName != null ? senderName : "A user",
                senderEmail,
                originalFilename,
                sizeMb,
                subjectHeader != null ? subjectHeader : "No subject",
                customMessage != null && !customMessage.isBlank() ? "<p style=\"margin: 4px 0;\"><strong>Message:</strong> " + customMessage + "</p>" : "",
                downloadUrl,
                merkleRoot != null ? merkleRoot : "ANCHORED_ON_CHAIN"
        );

        sendHtmlEmail(recipientEmail, emailSubject, htmlContent);
    }

    // --- SENDER CONFIRMATION EMAIL ---
    public void sendTransferSentConfirmationToSender(
            String senderEmail,
            String senderName,
            List<String> recipientEmails,
            String originalFilename,
            Long totalSizeBytes,
            String merkleRoot) {

        double sizeMb = totalSizeBytes != null ? (double) totalSizeBytes / (1024 * 1024) : 0.0;
        String subject = "✅ Transfer Complete: " + originalFilename;

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #10b981; margin-bottom: 8px;">Transfer Successfully Dispatched!</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your file <strong>%s</strong> (%.2f MB) has been chunked, multi-cloud distributed, and verified on Polygon Amoy testnet.</p>
                
                <div style="background-color: #1e293b; border-left: 4px solid #10b981; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0;"><strong>Recipients:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Merkle Root Hash:</strong> <span style="font-family: monospace; color: #a7f3d0;">%s</span></p>
                </div>
            </div>
            """.formatted(
                senderName != null ? senderName : senderEmail,
                originalFilename,
                sizeMb,
                String.join(", ", recipientEmails),
                merkleRoot != null ? merkleRoot : "PENDING_ANCHOR"
        );

        sendHtmlEmail(senderEmail, subject, htmlContent);
    }
}