package com.lampify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    public static final String PAYMENT_EXCHANGE = "payments.exchange";
    public static final String PAYMENT_QUEUE = "payments.notifications";
    public static final String PAYMENT_ROUTING_KEY = "payment.status";

    @Bean
    TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    Queue paymentNotificationsQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    Binding paymentStatusBinding(Queue paymentNotificationsQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentNotificationsQueue)
                .to(paymentsExchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
