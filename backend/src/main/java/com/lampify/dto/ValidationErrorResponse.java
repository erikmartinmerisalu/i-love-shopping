package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private boolean success = false;
    private String message;
    private Map<String, String> fieldErrors = new HashMap<>();
}
