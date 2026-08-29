package com.lampify.controller;

import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.UserRepository;
import com.lampify.security.JwtUtil;
import com.lampify.support.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BulkUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void seed() {
        databaseCleaner.resetCatalogData();
        userRepository.deleteAll();

        Category category = new Category();
        category.setName("Floor lamps");
        category.setSlug("floor-lamps");
        category.setDescription("Bulk upload target");
        categoryRepository.save(category);

        User admin = createUser("bulk-admin@test.local", UserRole.ADMIN, true);
        User customer = createUser("bulk-shopper@test.local", UserRole.CUSTOMER, false);
        adminToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());
        customerToken = jwtUtil.generateToken(customer.getEmail(), UserRole.CUSTOMER.name());
    }

    @Test
    void csvUploadCreatesValidRowAppliesFeaturedAndSkipsInvalidRows() throws Exception {
        String csv = """
                name,description,price,stockQuantity,brand,categorySlug,sku,active,featured
                Bulk Featured,Warm ambient desk lamp,49.99,25,LuminaTech,floor-lamps,BLK-FEAT-1,true,true
                Bad Row,missing-columns
                Unknown Cat,Desc,10.00,1,Brand,no-such-slug,BLK-UNK-1,true,false
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                csv.getBytes());

        mockMvc.perform(multipart("/admin/products/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2));

        Product featured = productRepository.findBySku("BLK-FEAT-1").orElseThrow();
        assertTrue(featured.isFeatured());
        assertEquals("Bulk Featured", featured.getName());
        assertTrue(productRepository.findBySku("BLK-UNK-1").isEmpty());
    }

    @Test
    void jsonUploadCreatesValidItemsAndSkipsUnknownCategory() throws Exception {
        String json = """
                [
                  {
                    "name": "JSON Lamp",
                    "description": "From JSON bulk",
                    "price": 29.99,
                    "stockQuantity": 10,
                    "brand": "JsonBrand",
                    "categorySlug": "floor-lamps",
                    "sku": "BLK-JSON-1",
                    "active": true,
                    "featured": true
                  },
                  {
                    "name": "JSON Unknown",
                    "description": "Missing category",
                    "price": 9.99,
                    "stockQuantity": 1,
                    "brand": "X",
                    "categorySlug": "does-not-exist",
                    "sku": "BLK-JSON-UNK",
                    "active": true,
                    "featured": false
                  }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.json",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes());

        mockMvc.perform(multipart("/admin/products/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors[0]").value(Matchers.containsString("Unknown category slug")));

        Product created = productRepository.findBySku("BLK-JSON-1").orElseThrow();
        assertTrue(created.isFeatured());
        assertEquals("JSON Lamp", created.getName());
    }

    @Test
    void customerCannotBulkUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                "name\n".getBytes());

        mockMvc.perform(multipart("/admin/products/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    private User createUser(String email, UserRole role, boolean twoFactorEnabled) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@", 2)[0]);
        user.setPassword(passwordEncoder.encode("Admin123!"));
        user.setRole(role);
        user.setEnabled(true);
        user.setPasswordLoginEnabled(true);
        user.setTwoFactorEnabled(twoFactorEnabled);
        if (twoFactorEnabled) {
            user.setTwoFactorSecret("TESTSECRET");
        }
        return userRepository.save(user);
    }
}
