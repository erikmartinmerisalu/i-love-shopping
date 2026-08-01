package com.lampify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {
    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Access token is required")
    private String accessToken;
}
