package com.lampify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.lampify.entity.Order;
import com.lampify.messaging.PaymentStatusEvent;

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
        sendEmail(toEmail, subject, body, "password reset");
    }

    public void sendOrderConfirmationEmail(Order order) {
        String subject = "ESTValgus order confirmation " + order.getOrderNumber();
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your order!\n\n")
                .append("Order number: ").append(order.getOrderNumber()).append("\n")
                .append("Status: ").append(order.getStatus()).append("\n")
                .append("Payment method: ").append(order.getPaymentMethod()).append("\n")
                .append("Ship to:\n")
                .append(order.getFullName()).append("\n")
                .append(order.getAddressLine1()).append("\n");
        if (order.getAddressLine2() != null && !order.getAddressLine2().isBlank()) {
            body.append(order.getAddressLine2()).append("\n");
        }
        body.append(order.getPostalCode()).append(" ").append(order.getCity()).append("\n")
                .append(order.getCountry()).append("\n")
                .append("Phone: ").append(order.getPhone()).append("\n\n")
                .append("Items:\n");
        order.getItems().forEach(item ->
                body.append("- ")
                        .append(item.getProductName())
                        .append(" x")
                        .append(item.getQuantity())
                        .append(" = €")
                        .append(item.getLineTotal())
                        .append("\n"));
        body.append("\nTotal: €").append(order.getTotalAmount()).append("\n")
                .append("\nYour payment was successful. Thank you for shopping with ESTValgus.\n");

        sendEmail(order.getEmail(), subject, body.toString(), "order confirmation");
    }

    public void sendPaymentSuccessEmail(PaymentStatusEvent event) {
        String subject = "ESTValgus payment successful — " + event.orderNumber();
        String body = "Your payment was successful.\n\n"
                + "Order number: " + event.orderNumber() + "\n"
                + "Status: " + event.orderStatus() + "\n"
                + "Amount: €" + event.amount() + "\n\n"
                + "Thank you for shopping with ESTValgus.\n";
        sendEmail(event.email(), subject, body, "payment success");
    }

    public void sendPaymentFailedEmail(PaymentStatusEvent event) {
        String subject = "ESTValgus payment failed — " + event.orderNumber();
        String body = "Unfortunately your payment could not be completed.\n\n"
                + "Order number: " + event.orderNumber() + "\n"
                + "Status: " + event.orderStatus() + "\n"
                + "Reason: " + (event.message() != null ? event.message() : "Payment failed") + "\n"
                + (event.failureCode() != null ? "Code: " + event.failureCode() + "\n" : "")
                + "\nStock for this order has been released. You can try checkout again.\n";
        sendEmail(event.email(), subject, body, "payment failure");
    }

    /**
     * Shared mail path for password reset, order confirmation, and payment notifications.
     * Uses the same MAIL_PROVIDER / MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM
     * settings (smtp or mailjet) — if forgot-password works, these emails work too.
     */
    private void sendEmail(String toEmail, String subject, String body, String kind) {
        String fromAddress = resolveFromAddress();
        if (!isMailConfigured(fromAddress)) {
            log.info("Mail not configured. {} for {}:\n{}", kind, toEmail, body);
            return;
        }

        try {
            if (usesMailjetApi()) {
                sendViaMailjetApi(fromAddress, toEmail, subject, body);
            } else {
                sendViaSmtp(fromAddress, toEmail, subject, body);
            }
            log.info("{} email sent to {}", kind, toEmail);
        } catch (Exception exception) {
            log.error("Failed to send {} email to {}: {}", kind, toEmail, exception.getMessage(), exception);
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
