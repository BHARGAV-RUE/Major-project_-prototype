package com.cloudtransfer.core.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TamperWarningEmailService extends BaseEmailService {

    public TamperWarningEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendTamperWarning(String toEmail, String name, String cloudProvider, int currentFlags) {
        String subject = "⚠️ WARNING FLAG #" + currentFlags + "/3: Data Tampering Detected on CloudTransfer Node";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #f59e0b; margin-bottom: 8px;">⚠️ Warning Flag #%d Issued</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Our Polygon blockchain hash audit detected chunk hash mismatch or deleted files on your <strong>%s</strong> storage vault.</p>
                
                <div style="background-color: #451a03; border: 1px solid #b45309; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0; color: #fde68a;"><strong>Current Warning Count:</strong> %d of 3 Warnings</p>
                    <p style="margin: 4px 0; color: #fcd34d;">Please ensure CloudTransfer chunk files in your cloud storage are not deleted or modified.</p>
                </div>
                
                <p style="font-size: 13px; color: #94a3b8;">
                    ⚠️ Exceeding 3 warning flags will trigger mandatory account termination within 4 business days.
                </p>
                <br/>
                <p style="font-size: 11px; color: #64748b;">CloudTransfer Security Operations</p>
            </div>
            """.formatted(currentFlags, name, cloudProvider, currentFlags);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}