package com.lampify.service;

import com.lampify.dto.AdminDeliveryOptionRequest;
import com.lampify.dto.DeliveryOptionDto;
import com.lampify.entity.DeliveryOption;
import com.lampify.repository.DeliveryOptionRepository;
import com.lampify.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeliveryOptionService {

    private final DeliveryOptionRepository deliveryOptionRepository;
    private final OrderRepository orderRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public DeliveryOptionService(
            DeliveryOptionRepository deliveryOptionRepository,
            OrderRepository orderRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.deliveryOptionRepository = deliveryOptionRepository;
        this.orderRepository = orderRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOptionDto> listActive() {
        return deliveryOptionRepository.findByActiveTrueOrderByPriceAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryOptionDto> listAllForAdmin() {
        adminAuthorizationService.requireAdminWithTwoFactor();
        return deliveryOptionRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DeliveryOptionDto create(AdminDeliveryOptionRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        DeliveryOption option = new DeliveryOption();
        apply(option, request);
        return toDto(deliveryOptionRepository.save(option));
    }

    @Transactional
    public DeliveryOptionDto update(Long id, AdminDeliveryOptionRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        DeliveryOption option = deliveryOptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery option not found"));
        apply(option, request);
        return toDto(deliveryOptionRepository.save(option));
    }

    @Transactional
    public void delete(Long id) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        DeliveryOption option = deliveryOptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery option not found"));
        if (orderRepository.countByDeliveryOption_Id(id) > 0) {
            option.setActive(false);
            deliveryOptionRepository.save(option);
            return;
        }
        deliveryOptionRepository.delete(option);
    }

    private void apply(DeliveryOption option, AdminDeliveryOptionRequest request) {
        option.setName(request.getName().trim());
        option.setPrice(request.getPrice());
        option.setEstimatedDays(request.getEstimatedDays());
        option.setActive(request.isActive());
    }

    private DeliveryOptionDto toDto(DeliveryOption option) {
        return new DeliveryOptionDto(
                option.getId(),
                option.getName(),
                option.getPrice(),
                option.getEstimatedDays(),
                option.isActive());
    }
}
