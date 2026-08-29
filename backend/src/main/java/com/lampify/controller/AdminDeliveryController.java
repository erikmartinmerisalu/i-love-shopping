package com.lampify.controller;

import com.lampify.dto.AdminDeliveryOptionRequest;
import com.lampify.dto.DeliveryOptionDto;
import com.lampify.service.DeliveryOptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/delivery-options")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final DeliveryOptionService deliveryOptionService;

    public AdminDeliveryController(DeliveryOptionService deliveryOptionService) {
        this.deliveryOptionService = deliveryOptionService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryOptionDto>> listDeliveryOptions() {
        return ResponseEntity.ok(deliveryOptionService.listAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<DeliveryOptionDto> createDeliveryOption(@Valid @RequestBody AdminDeliveryOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryOptionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryOptionDto> updateDeliveryOption(
            @PathVariable Long id,
            @Valid @RequestBody AdminDeliveryOptionRequest request) {
        return ResponseEntity.ok(deliveryOptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryOption(@PathVariable Long id) {
        deliveryOptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
