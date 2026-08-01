package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String message;
    private String email;
    private String username;
    private String accessToken;
    private String refreshToken;
    private boolean requires2fa;
    private boolean twoFactorEnabled;
    private String qrCodeUri;
    private List<String> backupCodes;
    private String provider;
    private boolean oauthAccount;
}
