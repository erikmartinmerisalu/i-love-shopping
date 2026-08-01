package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long productId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private int stockQuantity;
    private String imageUrl;
    private BigDecimal lineTotal;
}
