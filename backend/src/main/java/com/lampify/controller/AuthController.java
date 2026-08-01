package com.lampify.controller;

import com.lampify.dto.AuthRequest;
import com.lampify.dto.AuthResponse;
import com.lampify.dto.EmailRequest;
import com.lampify.dto.OAuthLoginRequest;
import com.lampify.dto.ResetPasswordRequest;
import com.lampify.dto.TwoFactorRequest;
import com.lampify.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.cookie.http-only:true}")
    private boolean httpOnlyCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSiteCookie;

    @Value("${app.cookie.max-age:604800}")
    private int cookieMaxAge;

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getAccessTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        String cookieValue = String.format(
                "refreshToken=%s; Path=/; Max-Age=%d; %s%s%s",
                refreshToken,
                cookieMaxAge,
                secureCookie ? "Secure; " : "",
                httpOnlyCookie ? "HttpOnly; " : "",
                "SameSite=" + sameSiteCookie
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        String clearCookie = String.format(
                "refreshToken=; Path=/; Max-Age=0; %s%sSameSite=%s",
                secureCookie ? "Secure; " : "",
                httpOnlyCookie ? "HttpOnly; " : "",
                sameSiteCookie
        );
        response.addHeader("Set-Cookie", clearCookie);
    }

    private ResponseEntity<AuthResponse> tokenResponse(AuthResponse authResponse, HttpServletResponse response) {
        if (authResponse.isSuccess() && authResponse.getRefreshToken() != null) {
            setRefreshTokenCookie(response, authResponse.getRefreshToken());
            authResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        if (authResponse.isSuccess()) {
            return tokenResponse(authResponse, response);
        }
        return ResponseEntity.badRequest().body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        if (authResponse.isSuccess()) {
            return tokenResponse(authResponse, response);
        }
        if (authResponse.isRequires2fa()) {
            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.badRequest().body(authResponse);
    }

    @PostMapping("/2fa/verify-login")
    public ResponseEntity<AuthResponse> verifyTwoFactorLogin(
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.verifyTwoFactorLogin(request);
        if (authResponse.isSuccess()) {
            return tokenResponse(authResponse, response);
        }
        return ResponseEntity.badRequest().body(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        if (authResponse.isSuccess()) {
            return tokenResponse(authResponse, response);
        }
        return ResponseEntity.status(401).body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        String accessToken = getAccessTokenFromHeader(request);
        AuthResponse authResponse = authService.logout(refreshToken, accessToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        AuthResponse response = authService.requestPasswordReset(request.getEmail());
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request.getResetToken(), request.getNewPassword());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<AuthResponse> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.oauthLogin(request.getProvider(), request.getAccessToken());
        if (authResponse.isSuccess()) {
            return tokenResponse(authResponse, response);
        }
        if (authResponse.isRequires2fa()) {
            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.badRequest().body(authResponse);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<AuthResponse> setupTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        AuthResponse response = authService.setupTwoFactor(request.getEmail(), request.getPassword());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/2fa/verify-setup")
    public ResponseEntity<AuthResponse> verifyTwoFactorSetup(@Valid @RequestBody TwoFactorRequest request) {
        AuthResponse response = authService.verifyTwoFactorSetup(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<AuthResponse> disableTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        AuthResponse response = authService.disableTwoFactor(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
