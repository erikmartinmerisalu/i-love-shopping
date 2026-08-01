package com.lampify.messaging;

import java.math.BigDecimal;

public record PaymentStatusEvent(
        String orderNumber,
        String email,
        boolean success,
        String orderStatus,
        String paymentStatus,
        String failureCode,
        String message,
        BigDecimal amount
) {
}
