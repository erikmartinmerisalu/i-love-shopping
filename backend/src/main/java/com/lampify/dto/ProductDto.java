package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String brand;
    private BigDecimal rating;
    private String category;
    private String categorySlug;
    private String primaryImageUrl;
    private BigDecimal weightKg;
    private BigDecimal weightLb;
    private BigDecimal lengthCm;
    private BigDecimal lengthIn;
    private BigDecimal widthCm;
    private BigDecimal widthIn;
    private BigDecimal heightCm;
    private BigDecimal heightIn;
}
