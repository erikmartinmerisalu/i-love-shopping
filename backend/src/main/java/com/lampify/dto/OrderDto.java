package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String orderNumber;
    private String status;
    private String paymentMethod;
    private String fullName;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
    private String country;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private Long deliveryOptionId;
    private String deliveryOptionName;
    private String estimatedDeliveryAt;
    private String createdAt;
    private List<OrderItemDto> items = new ArrayList<>();
    private List<OrderStatusHistoryDto> statusHistory = new ArrayList<>();
}
