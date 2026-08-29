package com.lampify.controller;

import com.lampify.entity.Category;
import com.lampify.entity.Order;
import com.lampify.entity.OrderItem;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.PaymentMethod;
import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.RefreshTokenRepository;
import com.lampify.repository.UserRepository;
import com.lampify.security.TokenHashes;
import com.lampify.service.AuthService;
import com.lampify.dto.AuthRequest;
import com.lampify.dto.AuthResponse;
import com.lampify.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class EncryptionAtRestTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void orderPiiAndLineItemsAreCiphertextInDatabase() {
        Category category = new Category();
        category.setName("Encryption Lamps");
        category.setSlug("encryption-lamps-" + System.nanoTime());
        category.setDescription("Test");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Secret Lamp");
        product.setDescription("Hidden");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(5);
        product.setBrand("Test");
        product.setRating(BigDecimal.ZERO);
        product.setCategory(category);
        product = productRepository.save(product);

        Order order = new Order();
        order.setOrderNumber("ENC-" + System.nanoTime());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setFullName("Ada Lovelace");
        order.setEmail("ada-encrypt@example.com");
        order.setPhone("+3725550000");
        order.setAddressLine1("Plaintext Street 1");
        order.setCity("Tallinn");
        order.setPostalCode("10111");
        order.setCountry("EE");
        order.setTotalAmount(new BigDecimal("10.00"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName("Secret Lamp");
        item.setUnitPrice(product.getPrice());
        item.setQuantity(1);
        item.setLineTotal(product.getPrice());
        order.getItems().add(item);
        order = orderRepository.saveAndFlush(order);

        String address = jdbcTemplate.queryForObject(
                "SELECT address_line1 FROM orders WHERE order_number = ?",
                String.class,
                order.getOrderNumber());
        String productName = jdbcTemplate.queryForObject(
                "SELECT product_name FROM order_items WHERE order_id = ?",
                String.class,
                order.getId());

        assertNotEquals("Plaintext Street 1", address);
        assertNotEquals("Secret Lamp", productName);
        assertTrue(isBase64Blob(address));
        assertTrue(isBase64Blob(productName));
    }

    @Test
    void twoFactorSecretIsCiphertextInDatabase() {
        User user = new User();
        String email = "2fa-encrypt-" + System.nanoTime() + "@example.com";
        user.setEmail(email);
        user.setUsername("twofa");
        user.setPassword(passwordEncoder.encode("StrongP@ss1"));
        user.setRole(UserRole.CUSTOMER);
        user.setEnabled(true);
        user.setPasswordLoginEnabled(true);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        user = userRepository.saveAndFlush(user);

        String stored = jdbcTemplate.queryForObject(
                "SELECT two_factor_secret FROM users WHERE id = ?",
                String.class,
                user.getId());

        assertNotEquals("JBSWY3DPEHPK3PXP", stored);
        assertTrue(isBase64Blob(stored));
        assertEquals("JBSWY3DPEHPK3PXP", userRepository.findById(user.getId()).orElseThrow().getTwoFactorSecret());
    }

    @Test
    void refreshTokenIsStoredHashed() {
        String email = "refresh-hash-" + System.nanoTime() + "@example.com";
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        AuthResponse response = authService.register(request);
        assertTrue(response.isSuccess());
        String rawToken = response.getRefreshToken();

        String stored = jdbcTemplate.queryForObject(
                "SELECT t.token FROM refresh_tokens t JOIN users u ON t.user_id = u.id WHERE u.email = ?",
                String.class,
                email.toLowerCase());

        assertNotEquals(rawToken, stored);
        assertEquals(TokenHashes.sha256Hex(rawToken), stored);
        assertEquals(1, refreshTokenRepository.findByToken(TokenHashes.sha256Hex(rawToken)).stream().count());
    }

    private static boolean isBase64Blob(String value) {
        return value != null && value.length() >= 24 && value.matches("^[A-Za-z0-9+/=]+$");
    }
}
