package com.cloudtransfer.core.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContributorForfeitEmailService extends BaseEmailService {

    public ContributorForfeitEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendForfeitNotification(String toEmail, String name, String cloudProvider) {
        String subject = "⚠️ CloudTransfer - Storage Node Forfeited";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #f43f5e; margin-bottom: 8px;">Storage Node Decommissioned</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>This email confirms that your <strong>%s</strong> contributor node has been successfully forfeited and unlinked from the CloudTransfer network.</p>
                
                <div style="background-color: #1e293b; border-left: 4px solid #f43f5e; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0; color: #f8fafc;">All assigned chunk data has been safely migrated to secondary replica nodes in the network.</p>
                </div>
                
                <p style="font-size: 13px; color: #94a3b8;">
                    If you did not initiate this forfeiture, please secure your account credentials immediately.
                </p>
                <br/>
                <p style="font-size: 11px; color: #64748b;">CloudTransfer Automated Systems</p>
            </div>
            """.formatted(name, cloudProvider);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}