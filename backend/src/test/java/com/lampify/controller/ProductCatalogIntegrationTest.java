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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category smartBulbs;
    private Category deskLamps;

    @BeforeEach
    void seedCatalog() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        smartBulbs = categoryRepository.save(category("Smart Bulbs", "smart-bulbs", "WiFi bulbs"));
        deskLamps = categoryRepository.save(category("Desk Lamps", "desk-lamps", "Task lighting"));

        productRepository.save(buildProduct("Smart LED Bulb", "WiFi RGB bulb", new BigDecimal("29.99"), smartBulbs, "LuminaTech", new BigDecimal("4.50")));
        productRepository.save(buildProduct("Smart Light Strip", "RGB strip", new BigDecimal("34.99"), smartBulbs, "LuminaTech", new BigDecimal("4.40")));
        productRepository.save(buildProduct("Desk Lamp", "Adjustable LED desk lamp", new BigDecimal("49.99"), deskLamps, "BrightWorks", new BigDecimal("4.30")));
        productRepository.save(buildProduct("Budget Desk Lamp", "Basic desk lamp", new BigDecimal("19.99"), deskLamps, "ArtiLite", new BigDecimal("3.80")));
    }

    @Test
    void listProductsReturnsPagedResultsWithFacets() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.products.length()").value(4))
                .andExpect(jsonPath("$.facets.categories.length()").value(2))
                .andExpect(jsonPath("$.facets.brands.length()").value(3));
    }

    @Test
    void searchFiltersByTextBrandAndCategory() throws Exception {
        mockMvc.perform(get("/products").param("search", "strip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Smart Light Strip"));

        mockMvc.perform(get("/products").param("category", "desk-lamps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/products").param("brand", "LuminaTech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void priceFilterAndSortingWork() throws Exception {
        mockMvc.perform(get("/products").param("minPrice", "30").param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/products").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].name").value("Budget Desk Lamp"))
                .andExpect(jsonPath("$.products[3].name").value("Desk Lamp"));

        mockMvc.perform(get("/products").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].rating").value(4.50));
    }

    @Test
    void categoriesAndProductDetailEndpointsWork() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").exists());

        Product product = productRepository.findAll().get(0);
        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.category.slug").value(smartBulbs.getSlug()))
                .andExpect(jsonPath("$.dimensions.weightKg").exists());
    }

    @Test
    void paginationRespectsPageAndSize() throws Exception {
        mockMvc.perform(get("/products").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4));
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
            BigDecimal rating) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(50);
        product.setBrand(brand);
        product.setRating(rating);
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
