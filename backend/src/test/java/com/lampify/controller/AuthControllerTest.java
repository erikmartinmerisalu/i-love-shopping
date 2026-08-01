package com.lampify.controller;

import com.lampify.dto.AuthResponse;
import com.lampify.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "secureCookie", false);
        ReflectionTestUtils.setField(authController, "httpOnlyCookie", true);
        ReflectionTestUtils.setField(authController, "sameSiteCookie", "Lax");
        ReflectionTestUtils.setField(authController, "cookieMaxAge", 604800);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void registerEndpointRejectsWeakPassword() throws Exception {
        AuthResponse failure = new AuthResponse();
        failure.setSuccess(false);
        failure.setMessage("Password must include uppercase, lowercase, number, and special character");
        when(authService.register(any())).thenReturn(failure);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password\",\"confirmPassword\":\"password\"}"))
                .andExpect(status().isBadRequest());
    }
}
