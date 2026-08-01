package com.lampify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    private String confirmPassword;
    private String captchaChallenge;
    private Integer captchaAnswer;
    private String provider;
    private String name;
    private String refreshToken;
    private String accessToken;
    private String resetToken;
    private String newPassword;
    private String twoFactorCode;
}
