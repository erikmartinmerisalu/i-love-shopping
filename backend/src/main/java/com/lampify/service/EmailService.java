package com.lampify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String MAILJET_SEND_URL = "https://api.mailjet.com/v3.1/send";

    private final JavaMailSender mailSender;
    private final RestClient restClient;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.mail.provider:smtp}")
    private String mailProvider;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.restClient = RestClient.create();
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "ESTValgus password reset";
        String body = "Use the link below to reset your ESTValgus password. This link expires in 1 hour.\n\n"
                + resetLink + "\n\nIf you did not request this, you can ignore this email.";

        String fromAddress = resolveFromAddress();
        if (!isMailConfigured(fromAddress)) {
            log.info("Mail not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        try {
            if (usesMailjetApi()) {
                sendViaMailjetApi(fromAddress, toEmail, subject, body);
            } else {
                sendViaSmtp(fromAddress, toEmail, subject, body);
            }
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception exception) {
            log.error("Failed to send password reset email to {}: {}", toEmail, exception.getMessage(), exception);
        }
    }

    private boolean isMailConfigured(String fromAddress) {
        return mailUsername != null
                && !mailUsername.isBlank()
                && mailPassword != null
                && !mailPassword.isBlank()
                && fromAddress != null
                && !fromAddress.isBlank();
    }

    private boolean usesMailjetApi() {
        return "mailjet".equalsIgnoreCase(mailProvider != null ? mailProvider.trim() : "");
    }

    private void sendViaSmtp(String fromAddress, String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendViaMailjetApi(String fromAddress, String toEmail, String subject, String body) {
        String credentials = mailUsername.trim() + ":" + mailPassword.trim();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> payload = Map.of(
                "Messages", List.of(Map.of(
                        "From", Map.of("Email", fromAddress, "Name", "ESTValgus"),
                        "To", List.of(Map.of("Email", toEmail)),
                        "Subject", subject,
                        "TextPart", body
                ))
        );

        restClient.post()
                .uri(MAILJET_SEND_URL)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private String resolveFromAddress() {
        if (mailFrom != null && !mailFrom.isBlank()) {
            return mailFrom.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank() && mailUsername.contains("@")) {
            return mailUsername.trim();
        }
        return null;
    }
}
