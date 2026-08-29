package com.lampify.controller;

import com.lampify.dto.DeliveryOptionDto;
import com.lampify.service.DeliveryOptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/delivery-options")
public class DeliveryOptionController {

    private final DeliveryOptionService deliveryOptionService;

    public DeliveryOptionController(DeliveryOptionService deliveryOptionService) {
        this.deliveryOptionService = deliveryOptionService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryOptionDto>> listActiveDeliveryOptions() {
        return ResponseEntity.ok(deliveryOptionService.listActive());
    }
}
