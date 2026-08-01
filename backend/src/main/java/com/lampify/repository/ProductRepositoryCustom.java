package com.lampify.repository;

import com.lampify.entity.Product;
import com.lampify.dto.ProductSearchCriteria;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(ProductSearchCriteria criteria);
    long countProducts(ProductSearchCriteria criteria);
    List<String> findDistinctBrands();
    BigDecimal findMinPrice();
    BigDecimal findMaxPrice();
}
