package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String email;
    private String username;
    private String role;
    private boolean enabled;
    private boolean twoFactorEnabled;
    private String createdAt;
}
