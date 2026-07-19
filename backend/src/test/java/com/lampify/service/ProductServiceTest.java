package com.lampify.service;

import com.lampify.dto.ProductListResponse;
import com.lampify.dto.ProductSearchCriteria;
import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductImageRepository;
import com.lampify.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    @Test
    void searchProductsClampsPageSizeAndBuildsResponse() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Desk Lamps");
        category.setSlug("desk-lamps");

        Product product = new Product();
        product.setId(1L);
        product.setName("Desk Lamp");
        product.setDescription("LED desk lamp");
        product.setPrice(new BigDecimal("49.99"));
        product.setStockQuantity(20);
        product.setBrand("BrightWorks");
        product.setRating(new BigDecimal("4.30"));
        product.setCategory(category);

        when(productRepository.searchProducts(any(ProductSearchCriteria.class))).thenReturn(List.of(product));
        when(productRepository.countProducts(any(ProductSearchCriteria.class))).thenReturn(1L);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productRepository.findDistinctBrands()).thenReturn(List.of("BrightWorks"));
        when(productRepository.findMinPrice()).thenReturn(new BigDecimal("19.99"));
        when(productRepository.findMaxPrice()).thenReturn(new BigDecimal("89.99"));

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setSearch("desk");
        criteria.setPage(0);
        criteria.setSize(500);

        ProductListResponse response = productService.searchProducts(criteria);

        assertEquals(1, response.getProducts().size());
        assertEquals("Desk Lamp", response.getProducts().get(0).getName());
        assertEquals(1, response.getTotalElements());
        assertEquals(100, criteria.getSize());
        assertEquals(1, response.getFacets().getBrands().size());
        verify(productRepository).searchProducts(criteria);
    }

    @Test
    void getProductByIdReturnsMappedDetail() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Smart Bulbs");
        category.setSlug("smart-bulbs");
        category.setDescription("WiFi bulbs");

        Product product = new Product();
        product.setId(5L);
        product.setName("Smart LED Bulb");
        product.setDescription("RGB bulb");
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(10);
        product.setBrand("LuminaTech");
        product.setRating(new BigDecimal("4.50"));
        product.setCategory(category);
        product.setWeightKg(new BigDecimal("0.120"));
        product.setWeightLb(new BigDecimal("0.265"));

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        var detail = productService.getProductById(5L);

        assertTrue(detail.isPresent());
        assertEquals("smart-bulbs", detail.get().getCategory().getSlug());
        assertEquals(new BigDecimal("0.120"), detail.get().getDimensions().getWeightKg());
    }
}
