package com.lampify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.dto.AuthRequest;
import com.lampify.dto.AuthResponse;
import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.UserRepository;
import com.lampify.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void adminWithoutTwoFactorCannotAccessDashboard() throws Exception {
        User admin = createAdmin("admin-no-2fa@test.local", "Admin123!", false);
        String token = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());

        mockMvc.perform(get("/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessAdminDashboard() throws Exception {
        User customer = createUser("shopper-403@test.local", "Shopper123!", UserRole.CUSTOMER);
        String token = jwtUtil.generateToken(customer.getEmail(), UserRole.CUSTOMER.name());

        mockMvc.perform(get("/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithTwoFactorCanAccessDashboard() throws Exception {
        User admin = createAdmin("secure-admin@test.local", "Admin123!", true);
        String token = jwtUtil.generateToken(admin.getEmail(), UserRole.ADMIN.name());

        mockMvc.perform(get("/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").exists());
    }

    @Test
    void adminLoginFlagsTwoFactorSetupWhenMissing() throws Exception {
        createAdmin("admin-flag@test.local", "Admin123!", false);

        AuthRequest request = new AuthRequest();
        request.setEmail("admin-flag@test.local");
        request.setPassword("Admin123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.requiresTwoFactorSetup").value(true));
    }

    private User createAdmin(String email, String password, boolean twoFactorEnabled) {
        return createUser(email, password, UserRole.ADMIN, twoFactorEnabled);
    }

    private User createUser(String email, String password, UserRole role) {
        return createUser(email, password, role, false);
    }

    private User createUser(String email, String password, UserRole role, boolean twoFactorEnabled) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@", 2)[0]);
        user.setPassword(passwordEncoder.encode(password));
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
