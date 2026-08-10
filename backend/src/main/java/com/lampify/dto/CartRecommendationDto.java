package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartRecommendationDto {
    private Long productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private int stockQuantity;
}
