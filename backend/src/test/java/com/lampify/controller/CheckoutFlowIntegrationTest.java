package com.lampify.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.PaymentTransactionRepository;
import com.lampify.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critical user flow: register, add to cart, checkout, sandbox payment, paid order.
 * With addFilters=false, Bearer tokens do not populate SecurityContext, so cart/order
 * use the guestCartToken cookie (register/login are still exercised).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CheckoutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Product product;

    @BeforeEach
    void seedProduct() {
        paymentTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Test Category");
        category.setSlug("test-category");
        category.setDescription("For checkout flow");
        category = categoryRepository.save(category);

        product = new Product();
        product.setName("Checkout Lamp");
        product.setDescription("Flow test lamp");
        product.setPrice(new BigDecimal("40.00"));
        product.setStockQuantity(5);
        product.setBrand("TestBrand");
        product.setRating(new BigDecimal("4.00"));
        product.setCategory(category);
        product = productRepository.save(product);
    }

    @Test
    void registrationCheckoutAndPaymentFlow() throws Exception {
        String email = "checkout.flow@example.com";
        String password = "StrongP@ss1";

        // Registration (critical auth flow)
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(email, password, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
        assertFalse(accessToken.isBlank());

        // Add to cart as guest (filters off => Bearer would not bind SecurityContext)
        MvcResult cartResult = mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": %d, "quantity": 2 }
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPrice").value(80.00))
                .andReturn();

        Cookie guestCartCookie = extractGuestCartCookie(cartResult.getResponse());
        assertNotNull(guestCartCookie, "guestCartToken cookie should be set for new guest cart");
        assertFalse(guestCartCookie.getValue().isBlank());

        // Place order with guest cart cookie
        MvcResult orderResult = mockMvc.perform(post("/orders")
                        .cookie(guestCartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Flow Tester",
                                  "email": "%s",
                                  "phone": "+37255551234",
                                  "addressLine1": "Test Street 1",
                                  "city": "Tallinn",
                                  "postalCode": "10111",
                                  "country": "Estonia",
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(80.00))
                .andReturn();

        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        String orderNumber = orderJson.path("orderNumber").asText();
        assertTrue(orderNumber.startsWith("EV-"));

        Product afterPlace = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(3, afterPlace.getStockQuantity());

        // Sandbox payment success
        mockMvc.perform(post("/payments/sandbox/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "orderNumber": "%s", "scenario": "success" }
                                """.formatted(orderNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));

        mockMvc.perform(get("/orders/" + orderNumber)
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.statusHistory.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // Cart cleared after place
        mockMvc.perform(get("/cart").cookie(guestCartCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void sandboxPaymentFailureThenRetrySucceeds() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": %d, "quantity": 1 }
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andReturn();

        Cookie guestCartCookie = extractGuestCartCookie(cartResult.getResponse());
        String email = "retry.pay@example.com";

        MvcResult orderResult = mockMvc.perform(post("/orders")
                        .cookie(guestCartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Retry Tester",
                                  "email": "%s",
                                  "phone": "+37255551234",
                                  "addressLine1": "Test Street 1",
                                  "city": "Tallinn",
                                  "postalCode": "10111",
                                  "country": "Estonia",
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn();

        String orderNumber = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("orderNumber")
                .asText();

        Product afterPlace = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(4, afterPlace.getStockQuantity());

        mockMvc.perform(post("/payments/sandbox/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "orderNumber": "%s", "scenario": "insufficient_funds" }
                                """.formatted(orderNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.orderStatus").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("insufficient_funds"));

        Product afterFailure = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, afterFailure.getStockQuantity());

        mockMvc.perform(get("/orders/" + orderNumber).param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(post("/payments/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "orderNumber": "%s" }
                                """.formatted(orderNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("sandbox"));

        mockMvc.perform(post("/payments/sandbox/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "orderNumber": "%s", "scenario": "success" }
                                """.formatted(orderNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));

        Product afterSuccess = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(4, afterSuccess.getStockQuantity());

        mockMvc.perform(get("/orders/" + orderNumber).param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.statusHistory[?(@.status == 'FAILED')]").exists())
                .andExpect(jsonPath("$.statusHistory[?(@.status == 'PAID')]").exists());
    }

    @Test
    void guestCanCancelPendingOrderWithCheckoutEmail() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": %d, "quantity": 1 }
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andReturn();

        Cookie guestCartCookie = extractGuestCartCookie(cartResult.getResponse());
        String email = "cancel.guest@example.com";

        MvcResult orderResult = mockMvc.perform(post("/orders")
                        .cookie(guestCartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Cancel Guest",
                                  "email": "%s",
                                  "phone": "+37255551234",
                                  "addressLine1": "Test Street 1",
                                  "city": "Tallinn",
                                  "postalCode": "10111",
                                  "country": "Estonia",
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderNumber = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("orderNumber")
                .asText();

        mockMvc.perform(post("/orders/" + orderNumber + "/cancel")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, afterCancel.getStockQuantity());
    }

    @Test
    void checkoutRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "bad",
                                  "phone": "",
                                  "addressLine1": "",
                                  "city": "",
                                  "postalCode": "",
                                  "country": "",
                                  "paymentMethod": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    /**
     * CartController sets the guest cookie via Set-Cookie header (not addCookie),
     * so MockHttpServletResponse#getCookie may be empty.
     */
    private static Cookie extractGuestCartCookie(MockHttpServletResponse response) {
        Cookie direct = response.getCookie(CartController.GUEST_CART_COOKIE);
        if (direct != null && direct.getValue() != null && !direct.getValue().isBlank()) {
            return direct;
        }
        for (String header : response.getHeaders("Set-Cookie")) {
            if (header == null || !header.startsWith(CartController.GUEST_CART_COOKIE + "=")) {
                continue;
            }
            String valuePart = header.substring((CartController.GUEST_CART_COOKIE + "=").length());
            int semi = valuePart.indexOf(';');
            String value = semi >= 0 ? valuePart.substring(0, semi) : valuePart;
            if (!value.isBlank()) {
                return new Cookie(CartController.GUEST_CART_COOKIE, value);
            }
        }
        return null;
    }
}