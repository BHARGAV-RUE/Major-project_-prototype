package com.cloudtransfer.core.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContributorRegistrationEmailService extends BaseEmailService {

    public ContributorRegistrationEmailService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public void sendRegistrationConfirmation(String toEmail, String name, String cloudProvider, Long pledgedBytes) {
        long pledgedGb = pledgedBytes != null ? pledgedBytes / (1024 * 1024 * 1024) : 0;
        String subject = "🌐 CloudTransfer - Contributor Node Registered";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px;">
                <h2 style="color: #06b6d4; margin-bottom: 8px;">Welcome to the CloudTransfer Network!</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Thank you for contributing your unused cloud storage to the CloudTransfer decentralized network pool.</p>
                
                <div style="background-color: #1e293b; border-left: 4px solid #06b6d4; padding: 16px; margin: 20px 0; border-radius: 8px;">
                    <p style="margin: 4px 0;"><strong>Cloud Provider:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Pledged Capacity:</strong> %d GB</p>
                    <p style="margin: 4px 0;"><strong>Status:</strong> <span style="color: #f59e0b; font-weight: bold;">INACTIVE (Pending Review)</span></p>
                </div>
                
                <p style="font-size: 13px; color: #94a3b8;">
                    ⏳ Your node is undergoing health verification. It will be reviewed by an Admin or automatically activated in <strong>4 business days</strong>.
                </p>
                <br/>
                <p style="font-size: 11px; color: #64748b;">CloudTransfer Automated Systems</p>
            </div>
            """.formatted(name, cloudProvider, pledgedGb);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}