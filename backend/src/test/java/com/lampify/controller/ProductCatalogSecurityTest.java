package com.lampify.controller;

import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCatalogSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seedCatalog() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Smart Bulbs");
        category.setSlug("smart-bulbs");
        category.setDescription("WiFi bulbs");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Smart LED Bulb");
        product.setDescription("RGB bulb");
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(10);
        product.setBrand("LuminaTech");
        product.setRating(new BigDecimal("4.50"));
        product.setCategory(category);
        productRepository.save(product);
    }

    @Test
    void productEndpointsAllowUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchSafelyHandlesSqlInjectionPayload() throws Exception {
        mockMvc.perform(get("/products").param("search", "'; DROP TABLE products; --"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Server error on SQL injection payload");
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void searchSafelyHandlesXssPayload() throws Exception {
        mockMvc.perform(get("/products").param("search", "<script>alert('xss')</script>"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Server error on XSS payload");
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void invalidProductIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownSortStillReturnsResults() throws Exception {
        mockMvc.perform(get("/products").param("sort", "not-a-real-sort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }
}
