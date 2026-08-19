package com.lampify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReviewRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long orderId;

    @Min(1)
    @Max(5)
    private int rating;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String body;
}
