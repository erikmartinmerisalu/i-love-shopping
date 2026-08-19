package com.lampify.controller;

import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.support.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class HomeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    void seedCatalog() {
        databaseCleaner.resetCatalogData();

        Category bulbs = categoryRepository.save(category("Smart Bulbs", "smart-bulbs", "WiFi bulbs"));
        Category lamps = categoryRepository.save(category("Desk Lamps", "desk-lamps", "Task lighting"));

        productRepository.save(buildProduct("Featured Bulb", "Top rated bulb", new BigDecimal("29.99"), bulbs, "LuminaTech", new BigDecimal("4.90"), true));
        productRepository.save(buildProduct("Featured Lamp", "Top rated lamp", new BigDecimal("49.99"), lamps, "BrightWorks", new BigDecimal("4.80"), true));
        productRepository.save(buildProduct("Budget Bulb", "Basic bulb", new BigDecimal("19.99"), bulbs, "ArtiLite", new BigDecimal("3.50"), false));
    }

    @Test
    void homeReturnsFeaturedProductsAndCollections() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProducts.length()").value(2))
                .andExpect(jsonPath("$.featuredProducts[0].name").exists())
                .andExpect(jsonPath("$.collections.length()").value(2))
                .andExpect(jsonPath("$.collections[0].category.slug").exists())
                .andExpect(jsonPath("$.collections[0].products.length()").value(2));
    }

    @Test
    void suggestReturnsMatchingProducts() throws Exception {
        mockMvc.perform(get("/products/suggest").param("q", "featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Featured Bulb"));

        mockMvc.perform(get("/products/suggest").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void relatedProductsExcludeSelf() throws Exception {
        Product product = productRepository.findAll().stream()
                .filter(item -> "Featured Bulb".equals(item.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/products/{id}/related", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Budget Bulb"));
    }

    @Test
    void contactFormAcceptsValidPayload() throws Exception {
        String body = """
                {
                  "name": "Jane Shopper",
                  "email": "jane@example.com",
                  "subject": "Delivery question",
                  "message": "When do you ship orders to Tartu?"
                }
                """;

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void contactFormRejectsInvalidPayload() throws Exception {
        String body = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "subject": "Hi",
                  "message": "short"
                }
                """;

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }

    private Category category(String name, String slug, String description) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return category;
    }

    private Product buildProduct(
            String name,
            String description,
            BigDecimal price,
            Category category,
            String brand,
            BigDecimal rating,
            boolean featured) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(50);
        product.setBrand(brand);
        product.setRating(rating);
        product.setFeatured(featured);
        product.setCategory(category);
        product.setWeightKg(new BigDecimal("1.000"));
        product.setWeightLb(new BigDecimal("2.205"));
        product.setLengthCm(new BigDecimal("10.00"));
        product.setLengthIn(new BigDecimal("3.94"));
        product.setWidthCm(new BigDecimal("10.00"));
        product.setWidthIn(new BigDecimal("3.94"));
        product.setHeightCm(new BigDecimal("20.00"));
        product.setHeightIn(new BigDecimal("7.87"));
        return product;
    }
}
