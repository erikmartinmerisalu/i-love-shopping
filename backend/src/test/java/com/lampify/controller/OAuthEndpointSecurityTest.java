package com.lampify.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oauthLoginIsPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"google","accessToken":"invalid-token"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
