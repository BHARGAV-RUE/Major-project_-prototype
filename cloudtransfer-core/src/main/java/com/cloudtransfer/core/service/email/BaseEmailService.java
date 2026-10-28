package com.cloudtransfer.core.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public abstract class BaseEmailService {

    protected final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@cloudtransfer.com}")
    protected String fromEmail;

    protected BaseEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    protected void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[{}] Email dispatched successfully to {}", this.getClass().getSimpleName(), toEmail);
        } catch (MessagingException e) {
            log.error("[{}] Failed to send email to {}: {}", this.getClass().getSimpleName(), toEmail, e.getMessage());
        } catch (Exception e) {
            log.warn("[{}] Mail dispatch bypassed or unconfigured: {}", this.getClass().getSimpleName(), e.getMessage());
        }
    }
}