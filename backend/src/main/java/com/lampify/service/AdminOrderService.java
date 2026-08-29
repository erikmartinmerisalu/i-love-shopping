package com.lampify.service;

import com.lampify.dto.AdminRefundRequest;
import com.lampify.dto.AdminUpdateOrderRequest;
import com.lampify.dto.DeliveryOptionDto;
import com.lampify.dto.OrderDto;
import com.lampify.dto.RefundDto;
import com.lampify.entity.DeliveryOption;
import com.lampify.entity.Order;
import com.lampify.entity.OrderItem;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.OrderStatusHistory;
import com.lampify.entity.Refund;
import com.lampify.entity.RefundStatus;
import com.lampify.repository.DeliveryOptionRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.RefundRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminOrderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OrderRepository orderRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final RefundRepository refundRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminOrderService(
            OrderRepository orderRepository,
            DeliveryOptionRepository deliveryOptionRepository,
            RefundRepository refundRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.orderRepository = orderRepository;
        this.deliveryOptionRepository = deliveryOptionRepository;
        this.refundRepository = refundRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(String status, int page, int size) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Order> orders;
        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = parseStatus(status);
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus, PageRequest.of(safePage, safeSize));
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));
        }
        return orders.getContent().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long id) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toDto(order);
    }

    @Transactional
    public OrderDto updateOrder(Long id, AdminUpdateOrderRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderStatus newStatus = parseStatus(request.getStatus());
        if (order.getStatus() != newStatus) {
            order.setStatus(newStatus);
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setStatus(newStatus);
            history.setNote("Updated by admin");
            order.getStatusHistory().add(history);
        }

        if (request.getDeliveryOptionId() != null) {
            DeliveryOption option = deliveryOptionRepository.findById(request.getDeliveryOptionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery option not found"));
            order.setDeliveryOption(option);
            order.setEstimatedDeliveryAt(LocalDateTime.now().plusDays(option.getEstimatedDays()));
        }

        return toDto(orderRepository.save(order));
    }

    @Transactional
    public RefundDto createRefund(Long orderId, AdminRefundRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus(RefundStatus.COMPLETED);
        Refund saved = refundRepository.save(refund);

        order.setStatus(OrderStatus.REFUNDED);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.REFUNDED);
        history.setNote("Refund issued");
        order.getStatusHistory().add(history);
        orderRepository.save(order);

        return toRefundDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryOptionDto> listDeliveryOptions() {
        adminAuthorizationService.requireAdminWithTwoFactor();
        return deliveryOptionRepository.findAll().stream().map(this::toDeliveryDto).toList();
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
    }

    private OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setFullName(order.getFullName());
        dto.setEmail(order.getEmail());
        dto.setPhone(order.getPhone());
        dto.setAddressLine1(order.getAddressLine1());
        dto.setAddressLine2(order.getAddressLine2());
        dto.setCity(order.getCity());
        dto.setPostalCode(order.getPostalCode());
        dto.setCountry(order.getCountry());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt().format(ISO));
        applyShipping(dto, order);
        dto.setItems(order.getItems().stream().map(this::toItemDto).toList());
        return dto;
    }

    private com.lampify.dto.OrderItemDto toItemDto(OrderItem item) {
        com.lampify.dto.OrderItemDto dto = new com.lampify.dto.OrderItemDto();
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private RefundDto toRefundDto(Refund refund) {
        return new RefundDto(
                refund.getId(),
                refund.getOrder().getId(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus().name(),
                refund.getCreatedAt().format(ISO));
    }

    private DeliveryOptionDto toDeliveryDto(DeliveryOption option) {
        return new DeliveryOptionDto(
                option.getId(),
                option.getName(),
                option.getPrice(),
                option.getEstimatedDays(),
                option.isActive());
    }

    private void applyShipping(OrderDto dto, Order order) {
        if (order.getDeliveryOption() == null) {
            return;
        }
        dto.setDeliveryOptionId(order.getDeliveryOption().getId());
        dto.setDeliveryOptionName(order.getDeliveryOption().getName());
        dto.setShippingAmount(order.getDeliveryOption().getPrice());
        if (order.getEstimatedDeliveryAt() != null) {
            dto.setEstimatedDeliveryAt(order.getEstimatedDeliveryAt().format(ISO));
        }
    }
}
