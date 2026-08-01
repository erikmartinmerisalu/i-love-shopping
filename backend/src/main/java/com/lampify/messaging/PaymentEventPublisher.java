package com.lampify.messaging;

import com.lampify.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final boolean messagingEnabled;

    public PaymentEventPublisher(
            ObjectProvider<RabbitTemplate> rabbitTemplate,
            @Value("${app.messaging.enabled:true}") boolean messagingEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingEnabled = messagingEnabled;
    }

    public void publish(PaymentStatusEvent event) {
        if (!messagingEnabled) {
            log.info("Messaging disabled — payment status event for {}: success={}",
                    event.orderNumber(), event.success());
            return;
        }

        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            log.warn("RabbitTemplate unavailable — skipping publish for {}", event.orderNumber());
            return;
        }

        try {
            template.convertAndSend(
                    RabbitConfig.PAYMENT_EXCHANGE,
                    RabbitConfig.PAYMENT_ROUTING_KEY,
                    event);
            log.info("Published payment status event for {} (success={})", event.orderNumber(), event.success());
        } catch (Exception exception) {
            log.error("Failed to publish payment status for {}: {}",
                    event.orderNumber(), exception.getMessage(), exception);
        }
    }
}
