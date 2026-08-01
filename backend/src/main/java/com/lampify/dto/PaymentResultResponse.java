package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {
    private boolean success;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;
    private String failureCode;
    private String message;
    private OrderDto order;
}
