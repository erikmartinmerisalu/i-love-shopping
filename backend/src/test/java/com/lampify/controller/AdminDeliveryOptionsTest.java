package com.lampify.controller;

import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDeliveryOptionsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

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

        User admin = createUser("delivery-admin@test.local", UserRole.ADMIN, true);
        User customer = createUser("delivery-shopper@test.local", UserRole.CUSTOMER, false);
        adminToken = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());
        customerToken = jwtUtil.generateToken(customer.getEmail(), UserRole.CUSTOMER.name());
    }

    @Test
    void adminCanCreateAndListDeliveryOption() throws Exception {
        MvcResult created = mockMvc.perform(post("/admin/delivery-options")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Overnight",
                                  "price": 19.99,
                                  "estimatedDays": 1,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Overnight"))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.estimatedDays").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = body.path("id").asLong();

        mockMvc.perform(get("/admin/delivery-options")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Overnight"));

        mockMvc.perform(get("/delivery-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Overnight"))
                .andExpect(jsonPath("$[0].price").value(19.99));
    }

    @Test
    void customerCannotManageDeliveryOptions() throws Exception {
        mockMvc.perform(get("/admin/delivery-options")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/delivery-options")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked",
                                  "price": 1.00,
                                  "estimatedDays": 3,
                                  "active": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutTwoFactorCannotManageDeliveryOptions() throws Exception {
        User admin = createUser("delivery-admin-no-2fa@test.local", UserRole.ADMIN, false);
        String token = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());

        mockMvc.perform(get("/admin/delivery-options")
                        .header("Authorization", "Bearer " + token))
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
