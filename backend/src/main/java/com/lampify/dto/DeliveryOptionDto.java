package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOptionDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private int estimatedDays;
    private boolean active;
}
