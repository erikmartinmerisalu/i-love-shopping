package com.lampify.dto;

import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String role;
    private Boolean enabled;
}
