package com.lampify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminProductRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @Min(0)
    private int stockQuantity;

    @NotBlank
    private String brand;

    @NotNull
    private Long categoryId;

    private String sku;
    private boolean active = true;
    private boolean featured;
    private BigDecimal weightKg;
    private BigDecimal weightLb;
    private BigDecimal lengthCm;
    private BigDecimal lengthIn;
    private BigDecimal widthCm;
    private BigDecimal widthIn;
    private BigDecimal heightCm;
    private BigDecimal heightIn;
}
