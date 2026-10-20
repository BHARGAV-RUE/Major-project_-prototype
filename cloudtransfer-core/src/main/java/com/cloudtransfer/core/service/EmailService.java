package com.cloudtransfer.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendTransferNotification(String recipientEmail, String senderName, String subject, String messageBody, String downloadLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject != null && !subject.isEmpty() ? subject : "New File Transfer from " + senderName);
            message.setText(
                    "Hello,\n\n" +
                            senderName + " has sent you files via CloudTransfer.\n\n" +
                            "Message: " + (messageBody != null ? messageBody : "None") + "\n\n" +
                            "You can securely access and download your files here:\n" + downloadLink + "\n\n" +
                            "Best regards,\nCloudTransfer Decentralized Network"
            );

            javaMailSender.send(message);
            log.info("[Email] Successfully sent transfer notification to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("[Email] Failed to send email to {}: {}", recipientEmail, e.getMessage());
        }
    }
}