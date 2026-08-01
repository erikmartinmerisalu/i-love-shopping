package com.lampify.service;

import com.lampify.dto.CheckoutRequest;
import com.lampify.dto.OrderDto;
import com.lampify.dto.OrderItemDto;
import com.lampify.dto.OrderStatusHistoryDto;
import com.lampify.dto.ValidationErrorResponse;
import com.lampify.entity.Cart;
import com.lampify.entity.CartItem;
import com.lampify.entity.Order;
import com.lampify.entity.OrderItem;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.OrderStatusHistory;
import com.lampify.entity.PaymentMethod;
import com.lampify.entity.Product;
import com.lampify.entity.User;
import com.lampify.repository.CartRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s()-]{7,20}$");
    private static final Pattern POSTAL_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\s-]{2,11}$");
    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("^[\\p{L}0-9\\s.,'/\\-]{5,255}$");

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public ValidationErrorResponse validateCheckout(CheckoutRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String fullName = trim(request.getFullName());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String addressLine1 = trim(request.getAddressLine1());
        String city = trim(request.getCity());
        String postalCode = trim(request.getPostalCode());
        String country = trim(request.getCountry());
        String paymentMethod = trim(request.getPaymentMethod());

        if (fullName == null || fullName.isBlank()) {
            fieldErrors.put("fullName", "Full name is required");
        } else if (fullName.length() < 2) {
            fieldErrors.put("fullName", "Full name must be at least 2 characters");
        }

        if (email == null || email.isBlank()) {
            fieldErrors.put("email", "Email is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            fieldErrors.put("email", "Invalid email format");
        }

        if (phone == null || phone.isBlank()) {
            fieldErrors.put("phone", "Phone is required");
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            fieldErrors.put("phone", "Invalid phone format");
        }

        if (addressLine1 == null || addressLine1.isBlank()) {
            fieldErrors.put("addressLine1", "Address is required");
        } else if (!ADDRESS_PATTERN.matcher(addressLine1).matches()) {
            fieldErrors.put("addressLine1", "Invalid address format");
        }

        String addressLine2 = trim(request.getAddressLine2());
        if (addressLine2 != null && !addressLine2.isBlank() && !ADDRESS_PATTERN.matcher(addressLine2).matches()) {
            fieldErrors.put("addressLine2", "Invalid address format");
        }

        if (city == null || city.isBlank()) {
            fieldErrors.put("city", "City is required");
        } else if (city.length() < 2) {
            fieldErrors.put("city", "Invalid city");
        }

        if (postalCode == null || postalCode.isBlank()) {
            fieldErrors.put("postalCode", "Postal code is required");
        } else if (!POSTAL_PATTERN.matcher(postalCode).matches()) {
            fieldErrors.put("postalCode", "Invalid postal code format");
        }

        if (country == null || country.isBlank()) {
            fieldErrors.put("country", "Country is required");
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            fieldErrors.put("paymentMethod", "Payment method is required");
        } else {
            try {
                PaymentMethod.valueOf(paymentMethod.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                fieldErrors.put("paymentMethod", "Invalid payment method");
            }
        }

        if (fieldErrors.isEmpty()) {
            return null;
        }

        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setSuccess(false);
        response.setMessage("Please fix the highlighted fields");
        response.setFieldErrors(fieldErrors);
        return response;
    }

    @Transactional
    public OrderDto placeOrder(String userEmail, String guestToken, CheckoutRequest request) {
        ValidationErrorResponse validation = validateCheckout(request);
        if (validation != null) {
            throw new CheckoutValidationException(validation);
        }

        Cart cart = loadCart(userEmail, guestToken);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        User user = userEmail != null
                ? userRepository.findByEmail(userEmail).orElse(null)
                : null;

        PaymentMethod paymentMethod = PaymentMethod.valueOf(
                request.getPaymentMethod().trim().toUpperCase(Locale.ROOT));

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(paymentMethod);
        order.setFullName(request.getFullName().trim());
        order.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        order.setPhone(request.getPhone().trim());
        order.setAddressLine1(request.getAddressLine1().trim());
        order.setAddressLine2(blankToNull(request.getAddressLine2()));
        order.setCity(request.getCity().trim());
        order.setPostalCode(request.getPostalCode().trim());
        order.setCountry(request.getCountry().trim());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Product no longer available: " + cartItem.getProduct().getName()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                String message = product.getStockQuantity() <= 0
                        ? product.getName() + " is out of stock"
                        : "Only " + product.getStockQuantity() + " left in stock for " + product.getName();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(lineTotal);
            order.getItems().add(orderItem);
        }

        order.setTotalAmount(total);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PENDING_PAYMENT);
        history.setNote("Order placed; awaiting payment");
        order.getStatusHistory().add(history);

        Order saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        emailService.sendOrderConfirmationEmail(saved);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(String userEmail, String statusFilter, String sort) {
        if (userEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required to view orders");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        OrderStatus status = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                status = OrderStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status filter");
            }
        }

        boolean ascending = "date_asc".equalsIgnoreCase(sort);
        List<Order> orders;
        if (status == null) {
            orders = ascending
                    ? orderRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
                    : orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        } else {
            orders = ascending
                    ? orderRepository.findByUserIdAndStatusOrderByCreatedAtAsc(user.getId(), status)
                    : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        }

        return orders.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        // Touch lazy status history inside the transaction
        order.getStatusHistory().size();
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderForViewer(String orderNumber, String userEmail, String emailHint) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.getStatusHistory().size();
        assertCanView(order, userEmail, emailHint);
        return toDto(order);
    }

    /**
     * Cancel unprocessed orders (PENDING_PAYMENT only) and restore stock.
     */
    @Transactional
    public OrderDto cancelOrder(String orderNumber, String userEmail, String emailHint) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanView(order, userEmail, emailHint);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only unprocessed orders (awaiting payment) can be cancelled");
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElse(null);
            if (product == null) {
                continue;
            }
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CANCELLED);
        history.setNote("Order cancelled by customer; stock restored");
        order.getStatusHistory().add(history);
        order.getStatusHistory().size();
        return toDto(orderRepository.save(order));
    }

    private void assertCanView(Order order, String userEmail, String emailHint) {
        if (userEmail != null) {
            if (order.getUser() != null && userEmail.equalsIgnoreCase(order.getUser().getEmail())) {
                return;
            }
            if (order.getEmail() != null && userEmail.equalsIgnoreCase(order.getEmail())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this order");
        }
        if (emailHint != null && !emailHint.isBlank()
                && order.getEmail() != null
                && emailHint.trim().equalsIgnoreCase(order.getEmail())) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Login or provide the order email to view this order");
    }

    /**
     * Marks order paid. Idempotent if already PAID.
     * Stock was reserved at place-order under pessimistic locks — payment does not decrement again.
     */
    @Transactional
    public OrderDto markPaymentSucceeded(String orderNumber, String note) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.PAID) {
            return toDto(order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be marked paid from status " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PAID);
        history.setNote(note != null ? note : "Payment succeeded");
        order.getStatusHistory().add(history);
        return toDto(orderRepository.save(order));
    }

    /**
     * Marks payment failed and restores reserved stock (pessimistic locks prevent concurrent races).
     */
    @Transactional
    public OrderDto markPaymentFailed(String orderNumber, String failureCode, String failureMessage) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already paid");
        }
        if (order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.CANCELLED) {
            return toDto(order);
        }

        // Restock under row locks so concurrent payment outcomes cannot double-adjust inventory.
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElse(null);
            if (product == null) {
                continue;
            }
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.FAILED);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.FAILED);
        String note = failureMessage != null ? failureMessage : "Payment failed";
        if (failureCode != null && !failureCode.isBlank()) {
            note = failureCode + ": " + note;
        }
        history.setNote(note);
        order.getStatusHistory().add(history);
        return toDto(orderRepository.save(order));
    }

    private Cart loadCart(String userEmail, String guestToken) {
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
            return cartRepository.findByUserIdWithItems(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
        }
        if (guestToken == null || guestToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }
        return cartRepository.findByGuestTokenWithItems(guestToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
    }

    private String generateOrderNumber() {
        String candidate;
        do {
            candidate = "EV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
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
        dto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO) : null);
        dto.setItems(order.getItems().stream().map(this::toItemDto).toList());
        dto.setStatusHistory(order.getStatusHistory().stream().map(this::toHistoryDto).toList());
        return dto;
    }

    private OrderDto toSummaryDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setFullName(order.getFullName());
        dto.setEmail(order.getEmail());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO) : null);
        dto.setItems(List.of());
        dto.setStatusHistory(List.of());
        return dto;
    }

    private OrderItemDto toItemDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private OrderStatusHistoryDto toHistoryDto(OrderStatusHistory history) {
        OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
        dto.setStatus(history.getStatus().name());
        dto.setNote(history.getNote());
        dto.setCreatedAt(history.getCreatedAt() != null ? history.getCreatedAt().format(ISO) : null);
        return dto;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static class CheckoutValidationException extends RuntimeException {
        private final ValidationErrorResponse response;

        public CheckoutValidationException(ValidationErrorResponse response) {
            super(response.getMessage());
            this.response = response;
        }

        public ValidationErrorResponse getResponse() {
            return response;
        }
    }
}
