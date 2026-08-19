package com.lampify.service;

import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthorizationService {

    private final UserRepository userRepository;

    public AdminAuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireAdminWithTwoFactor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin authentication required");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account not found"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        if (!user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Two-factor authentication is required for admin access");
        }

        return user;
    }
}
