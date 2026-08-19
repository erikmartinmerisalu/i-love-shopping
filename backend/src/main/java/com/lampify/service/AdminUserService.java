package com.lampify.service;

import com.lampify.dto.AdminUpdateUserRequest;
import com.lampify.dto.AdminUserDto;
import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminUserService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminUserService(
            UserRepository userRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers(int page, int size) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));
        return users.getContent().stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminUserDto updateUser(Long id, AdminUpdateUserRequest request, String actingAdminEmail) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getRole() != null) {
            UserRole newRole = parseRole(request.getRole());
            if (user.getRole() == UserRole.ADMIN && newRole == UserRole.CUSTOMER) {
                long adminCount = userRepository.countByRole(UserRole.ADMIN);
                if (adminCount <= 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last admin");
                }
            }
            user.setRole(newRole);
        }

        if (request.getEnabled() != null) {
            if (user.getEmail().equalsIgnoreCase(actingAdminEmail) && !request.getEnabled()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot disable your own account");
            }
            user.setEnabled(request.getEnabled());
        }

        return toDto(userRepository.save(user));
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.isEnabled(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt().format(ISO));
    }
}
