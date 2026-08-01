package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String orderNumber;
    private String mode; // stripe | sandbox
    private String publishableKey;
    private String clientSecret;
    private String providerPaymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
