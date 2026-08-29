package com.lampify.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BulkProductRow {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String brand;
    private String categorySlug;
    private String sku;
    private Boolean active;
    private Boolean featured;
}
