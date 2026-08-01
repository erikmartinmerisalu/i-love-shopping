package com.lampify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SandboxPaymentRequest {

    @NotBlank
    private String orderNumber;

    /**
     * success | insufficient_funds | invalid_card | expired_card | timeout
     */
    @NotBlank
    private String scenario;
}
