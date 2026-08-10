package com.lampify.messaging;

import com.lampify.config.RabbitConfig;
import com.lampify.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationListener.class);

    private final EmailService emailService;

    public PaymentNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void onPaymentStatus(PaymentStatusEvent event) {
        log.info("Received payment status for {} success={}", event.orderNumber(), event.success());
        // Order confirmation email is sent synchronously when payment succeeds (OrderService).
        if (!event.success()) {
            emailService.sendPaymentFailedEmail(event);
        }
    }
}
