package com.lampify.controller;

import com.lampify.dto.AdminUpdateUserRequest;
import com.lampify.dto.AdminUserDto;
import com.lampify.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.listUsers(page, size));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        String actingAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminUserService.updateUser(id, request, actingAdminEmail));
    }
}
