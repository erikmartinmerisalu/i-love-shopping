package com.lampify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCategoryRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;
}
