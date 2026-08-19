package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionDto {
    private Long id;
    private String name;
    private String categorySlug;
    private BigDecimal price;
    private String primaryImageUrl;
}
