package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCriteria {
    private String search;
    private String category;
    private String brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sort = "relevance";
    private int page = 0;
    private int size = 20;
}
