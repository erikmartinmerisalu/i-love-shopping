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
import java.time.LocalDateTime;

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
    @Autowired private ReviewRepository reviewRepository;
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
        order = paidOrder(customer, "REV-ORD-001");
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
                .andExpect(jsonPath("$[0].productName").value("Review Lamp"))
                .andExpect(jsonPath("$[0].authorUsername").value("reviewer"))
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

    @Test
    void helpfulVoteMovesReviewAboveNewerUnvotedReview() throws Exception {
        User secondBuyer = createUser("reviewer-two@example.com", UserRole.CUSTOMER);
        String secondToken = jwtUtil.generateToken(secondBuyer.getEmail(), UserRole.CUSTOMER.name());
        Order secondOrder = paidOrder(secondBuyer, "REV-ORD-002");

        long olderId = submitAndApprove(
                customerToken,
                order.getId(),
                4,
                "Older review that will receive a helpful vote.");
        long newerId = submitAndApprove(
                secondToken,
                secondOrder.getId(),
                5,
                "Newer review with no helpful votes yet at all.");

        Review older = reviewRepository.findById(olderId).orElseThrow();
        older.setCreatedAt(LocalDateTime.now().minusHours(2));
        reviewRepository.save(older);

        mockMvc.perform(post("/reviews/" + olderId + "/helpful")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulCount").value(1))
                .andExpect(jsonPath("$.helpfulByCurrentUser").value(true));

        mockMvc.perform(get("/products/" + product.getId() + "/reviews").param("sort", "helpful"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.reviews[0].id").value(olderId))
                .andExpect(jsonPath("$.reviews[0].helpfulCount").value(1))
                .andExpect(jsonPath("$.reviews[1].id").value(newerId))
                .andExpect(jsonPath("$.reviews[1].helpfulCount").value(0));

        mockMvc.perform(get("/products/" + product.getId() + "/reviews").param("sort", "recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].id").value(newerId))
                .andExpect(jsonPath("$.reviews[1].id").value(olderId));

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(2));
    }

    @Test
    void paidOrderShowsReviewableUntilSubmittedThenShowsPendingStatus() throws Exception {
        mockMvc.perform(get("/orders/" + order.getOrderNumber())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.items[0].canReview").value(true));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "orderId": %d,
                                  "rating": 4,
                                  "body": "Bright lamp, solid build, would buy again."
                                }
                                """.formatted(product.getId(), order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/orders/" + order.getOrderNumber())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].canReview").value(false))
                .andExpect(jsonPath("$.items[0].reviewStatus").value("PENDING"));
    }

    @Test
    void guestPaidOrderBecomesReviewableWhenBuyerSignsIn() throws Exception {
        Order guestOrder = paidOrder(null, "REV-GUEST-001");
        guestOrder.setEmail(customer.getEmail());
        guestOrder = orderRepository.save(guestOrder);

        mockMvc.perform(get("/orders/" + guestOrder.getOrderNumber())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].canReview").value(true));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "orderId": %d,
                                  "rating": 5,
                                  "body": "Guest checkout lamp is now reviewable after login."
                                }
                                """.formatted(product.getId(), guestOrder.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private long submitAndApprove(String token, long orderId, int rating, String body) throws Exception {
        var submitted = mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "orderId": %d,
                                  "rating": %d,
                                  "body": "%s"
                                }
                                """.formatted(product.getId(), orderId, rating, body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long reviewId = objectMapper.readTree(submitted.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/admin/reviews/" + reviewId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        return reviewId;
    }

    private Order paidOrder(User buyer, String orderNumber) {
        Order paid = new Order();
        paid.setOrderNumber(orderNumber);
        paid.setUser(buyer);
        paid.setStatus(OrderStatus.PAID);
        paid.setPaymentMethod(PaymentMethod.CARD);
        paid.setFullName("Test Buyer");
        paid.setEmail(buyer != null ? buyer.getEmail() : "guest-buyer@example.com");
        paid.setPhone("+3725555555");
        paid.setAddressLine1("Test St 1");
        paid.setCity("Tallinn");
        paid.setPostalCode("10111");
        paid.setCountry("EE");
        paid.setTotalAmount(new BigDecimal("55.00"));

        OrderItem item = new OrderItem();
        item.setOrder(paid);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(1);
        item.setLineTotal(product.getPrice());
        paid.getItems().add(item);
        return orderRepository.save(paid);
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
