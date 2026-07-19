package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {
    private List<ProductDto> products;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private FacetsDto facets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetsDto {
        private List<CategoryDto> categories;
        private List<String> brands;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
    }
}
