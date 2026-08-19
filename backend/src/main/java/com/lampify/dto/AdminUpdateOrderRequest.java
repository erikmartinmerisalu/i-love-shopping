package com.lampify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateOrderRequest {
    @NotBlank
    private String status;

    private Long deliveryOptionId;
}
