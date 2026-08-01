package com.lampify.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.dto.AuthRequest;
import com.lampify.dto.EmailRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthenticationFlow() throws Exception {
        String email = "integration.user@example.com";
        String password = "StrongP@ss1";

        register(email, password);
        SessionState session = login(email, password);
        assertNotNull(session.accessToken());
        assertNotNull(session.refreshCookie());

        String refreshedAccessToken = refresh(session.refreshCookie());
        assertNotNull(refreshedAccessToken);

        logout(session.refreshCookie());
        mockMvc.perform(post("/auth/refresh")
                        .cookie(session.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotationRejectsOldToken() throws Exception {
        String email = "rotation.user@example.com";
        String password = "StrongP@ss1";

        register(email, password);
        SessionState session = login(email, password);

        Cookie newRefreshCookie = refreshAndGetCookie(session.refreshCookie());
        assertNotNull(newRefreshCookie);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(session.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been invalidated"));
    }

    @Test
    void passwordResetFlow() throws Exception {
        String email = "reset.user@example.com";
        String password = "StrongP@ss1";
        String newPassword = "NewPass1!";

        register(email, password);

        EmailRequest forgotRequest = new EmailRequest();
        forgotRequest.setEmail(email);

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resetToken":"invalid-token","newPassword":"%s"}
                                """.formatted(newPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void register(String email, String password) throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(password);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private SessionState login(String email, String password) throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new SessionState(
                body.get("accessToken").asText(),
                result.getResponse().getCookie("refreshToken")
        );
    }

    private String refresh(Cookie refreshCookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Cookie refreshAndGetCookie(Cookie refreshCookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("refreshToken");
    }

    private void logout(Cookie refreshCookie) throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private AuthRequest requestWithEmail(String email) {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword("StrongP@ss1");
        return request;
    }

    private record SessionState(String accessToken, Cookie refreshCookie) {}
}
