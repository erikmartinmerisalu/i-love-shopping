package com.lampify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(nullable = true)
    private String provider;

    @Column(name = "password_login_enabled", nullable = false)
    private boolean passwordLoginEnabled = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(nullable = true)
    private String twoFactorSecret;

    @Column(nullable = true)
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = true)
    private LocalDateTime accountLockedUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (username == null || username.isBlank()) {
            username = email != null ? email.split("@", 2)[0] : "user";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
