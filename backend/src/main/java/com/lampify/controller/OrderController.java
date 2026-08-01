package com.lampify.controller;

import com.lampify.dto.ApiErrorResponse;
import com.lampify.dto.CheckoutRequest;
import com.lampify.dto.OrderDto;
import com.lampify.dto.ValidationErrorResponse;
import com.lampify.service.OrderService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        try {
            OrderDto order = orderService.placeOrder(currentUserEmail(), guestToken(httpRequest), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (OrderService.CheckoutValidationException ex) {
            return ResponseEntity.badRequest().body(ex.getResponse());
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "date_desc") String sort) {
        try {
            List<OrderDto> orders = orderService.listOrders(currentUserEmail(), status, sort);
            return ResponseEntity.ok(orders);
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String email) {
        try {
            OrderDto order = orderService.getOrderForViewer(orderNumber, currentUserEmail(), email);
            return ResponseEntity.ok(order);
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String email) {
        try {
            OrderDto order = orderService.cancelOrder(orderNumber, currentUserEmail(), email);
            return ResponseEntity.ok(order);
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setSuccess(false);
        response.setMessage("Please fix the highlighted fields");
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<ApiErrorResponse> error(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(false, ex.getReason() != null ? ex.getReason() : "Order request failed"));
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private String guestToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (CartController.GUEST_CART_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
