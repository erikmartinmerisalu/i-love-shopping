package com.lampify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.entity.*;
import com.lampify.repository.*;
import com.lampify.security.JwtUtil;
import com.lampify.support.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDatabaseCleaner databaseCleaner;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Product product;
    private User customer;
    private User admin;
    private Order order;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void seed() {
        databaseCleaner.resetCatalogData();
        userRepository.deleteAll();

        Category category = new Category();
        category.setName("Review Category");
        category.setSlug("review-category");
        category.setDescription("For review tests");
        category = categoryRepository.save(category);

        product = new Product();
        product.setName("Review Lamp");
        product.setDescription("Test lamp");
        product.setPrice(new BigDecimal("55.00"));
        product.setStockQuantity(10);
        product.setBrand("TestBrand");
        product.setRating(BigDecimal.ZERO);
        product.setCategory(category);
        product = productRepository.save(product);

        customer = createUser("reviewer@example.com", UserRole.CUSTOMER);
        admin = createAdmin("review-admin@test.local", true);
        customerToken = jwtUtil.generateToken(customer.getEmail(), UserRole.CUSTOMER.name());
        adminToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());

        order = new Order();
        order.setOrderNumber("REV-ORD-001");
        order.setUser(customer);
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setFullName("Test Buyer");
        order.setEmail(customer.getEmail());
        order.setPhone("+3725555555");
        order.setAddressLine1("Test St 1");
        order.setCity("Tallinn");
        order.setPostalCode("10111");
        order.setCountry("EE");
        order.setTotalAmount(new BigDecimal("55.00"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(1);
        item.setLineTotal(product.getPrice());
        order.getItems().add(item);
        order = orderRepository.save(order);
    }

    @Test
    void submitApproveAndListReviewFlow() throws Exception {
        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "orderId": %d,
                                  "rating": 5,
                                  "body": "Excellent lamp, easy to install and looks great."
                                }
                                """.formatted(product.getId(), order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/products/" + product.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        var listPending = mockMvc.perform(get("/admin/reviews")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andReturn();

        long reviewId = objectMapper.readTree(listPending.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(post("/admin/reviews/" + reviewId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/products/" + product.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.reviews[0].rating").value(5));

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.rating").value(5.0));
    }

    private User createUser(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setPassword(passwordEncoder.encode("StrongP@ss1"));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private User createAdmin(String email, boolean twoFactorEnabled) {
        User user = createUser(email, UserRole.ADMIN);
        user.setTwoFactorEnabled(twoFactorEnabled);
        if (twoFactorEnabled) {
            user.setTwoFactorSecret("TESTSECRET");
        }
        return userRepository.save(user);
    }
}
