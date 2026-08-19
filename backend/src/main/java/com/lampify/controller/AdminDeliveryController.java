package com.lampify.controller;

import com.lampify.dto.DeliveryOptionDto;
import com.lampify.service.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/delivery-options")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final AdminOrderService adminOrderService;

    public AdminDeliveryController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryOptionDto>> listDeliveryOptions() {
        return ResponseEntity.ok(adminOrderService.listDeliveryOptions());
    }
}
