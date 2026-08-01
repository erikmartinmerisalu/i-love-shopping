package com.lampify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.dto.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerSafelyHandlesSqlInjectionPayload() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("inject';--@example.com");
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Server error on SQL injection payload");
                    }
                });
    }

    @Test
    void registerSafelyHandlesXssPayloadInEmail() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("<script>alert('xss')</script>@example.com");
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Server error on XSS payload");
                    }
                });
    }

    @Test
    void refreshRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .header("Cookie", "refreshToken=not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void oauthRejectsInvalidGoogleToken() throws Exception {
        mockMvc.perform(post("/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"google","accessToken":"invalid-token"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("security.user@example.com");
        request.setPassword("weakpass");
        request.setConfirmPassword("weakpass");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("uppercase")));
    }
}
