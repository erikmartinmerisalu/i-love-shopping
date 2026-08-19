package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String createdAt;
}
