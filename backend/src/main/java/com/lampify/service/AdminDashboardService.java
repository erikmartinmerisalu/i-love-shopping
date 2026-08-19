package com.lampify.service;

import com.lampify.dto.AdminDashboardDto;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.UserRole;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminDashboardService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        adminAuthorizationService.requireAdminWithTwoFactor();
        AdminDashboardDto dto = new AdminDashboardDto();
        dto.setTotalProducts(productRepository.count());
        dto.setActiveProducts(productRepository.countByActiveTrue());
        dto.setLowStockProducts(productRepository.countByStockQuantityLessThanEqual(LOW_STOCK_THRESHOLD));
        dto.setTotalOrders(orderRepository.count());
        dto.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT));
        dto.setTotalUsers(userRepository.count());
        dto.setAdminUsers(userRepository.countByRole(UserRole.ADMIN));
        return dto;
    }
}
