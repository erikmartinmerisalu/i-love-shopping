package com.lampify.controller;

import com.lampify.dto.AdminRefundRequest;
import com.lampify.dto.AdminUpdateOrderRequest;
import com.lampify.dto.DeliveryOptionDto;
import com.lampify.dto.OrderDto;
import com.lampify.dto.RefundDto;
import com.lampify.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminOrderService.listOrders(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getOrder(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateOrderRequest request) {
        return ResponseEntity.ok(adminOrderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/refunds")
    public ResponseEntity<RefundDto> createRefund(
            @PathVariable Long id,
            @Valid @RequestBody AdminRefundRequest request) {
        return ResponseEntity.ok(adminOrderService.createRefund(id, request));
    }
}
