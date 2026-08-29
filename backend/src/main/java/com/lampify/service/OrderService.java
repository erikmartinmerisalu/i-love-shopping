package com.lampify.service;

import com.lampify.dto.CheckoutRequest;
import com.lampify.dto.OrderDto;
import com.lampify.dto.OrderItemDto;
import com.lampify.dto.OrderStatusHistoryDto;
import com.lampify.dto.ValidationErrorResponse;
import com.lampify.entity.Cart;
import com.lampify.entity.CartItem;
import com.lampify.entity.DeliveryOption;
import com.lampify.entity.Order;
import com.lampify.entity.OrderItem;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.OrderStatusHistory;
import com.lampify.entity.PaymentMethod;
import com.lampify.entity.Product;
import com.lampify.entity.User;
import com.lampify.repository.CartRepository;
import com.lampify.repository.DeliveryOptionRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.ReviewRepository;
import com.lampify.repository.UserRepository;
import com.lampify.validation.CheckoutFieldValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final List<OrderStatus> REVIEWABLE_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.FULFILLED);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            DeliveryOptionRepository deliveryOptionRepository,
            ReviewRepository reviewRepository,
            EmailService emailService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.deliveryOptionRepository = deliveryOptionRepository;
        this.reviewRepository = reviewRepository;
        this.emailService = emailService;
    }

    public ValidationErrorResponse validateCheckout(CheckoutRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String paymentMethod = CheckoutFieldValidator.normalize(request.getPaymentMethod());

        CheckoutFieldValidator.collectFieldErrors(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getCity(),
                request.getPostalCode(),
                request.getCountry(),
                fieldErrors);

        if (paymentMethod == null || paymentMethod.isBlank()) {
            fieldErrors.put("paymentMethod", "Payment method is required");
        } else {
            try {
                PaymentMethod.valueOf(paymentMethod.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                fieldErrors.put("paymentMethod", "Invalid payment method");
            }
        }

        if (request.getDeliveryOptionId() == null) {
            fieldErrors.put("deliveryOptionId", "Shipping option is required");
        } else {
            DeliveryOption option = deliveryOptionRepository.findById(request.getDeliveryOptionId()).orElse(null);
            if (option == null || !option.isActive()) {
                fieldErrors.put("deliveryOptionId", "Invalid shipping option");
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
                CheckoutFieldValidator.normalize(request.getPaymentMethod()).toUpperCase(Locale.ROOT));

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(paymentMethod);
        order.setFullName(CheckoutFieldValidator.normalize(request.getFullName()));
        order.setEmail(CheckoutFieldValidator.normalize(request.getEmail()).toLowerCase(Locale.ROOT));
        order.setPhone(CheckoutFieldValidator.normalizePhone(request.getPhone()));
        order.setAddressLine1(CheckoutFieldValidator.normalize(request.getAddressLine1()));
        order.setAddressLine2(blankToNull(request.getAddressLine2()));
        order.setCity(CheckoutFieldValidator.normalize(request.getCity()));
        order.setPostalCode(CheckoutFieldValidator.normalize(request.getPostalCode()));
        order.setCountry(CheckoutFieldValidator.normalize(request.getCountry()));

        DeliveryOption deliveryOption = deliveryOptionRepository.findById(request.getDeliveryOptionId())
                .filter(DeliveryOption::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipping option"));
        order.setDeliveryOption(deliveryOption);
        order.setEstimatedDeliveryAt(LocalDateTime.now().plusDays(deliveryOption.getEstimatedDays()));

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

    @Transactional
    public OrderDto getOrderForViewer(String orderNumber, String userEmail, String emailHint) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.getStatusHistory().size();
        claimGuestOrderIfEligible(order, userEmail);
        assertCanView(order, userEmail, emailHint);
        return toDto(order);
    }

    /**
     * Guest checkouts store no user_id. If the signed-in account uses the same email,
     * attach the order so reviews and "My orders" work.
     */
    private void claimGuestOrderIfEligible(Order order, String userEmail) {
        if (order.getUser() != null || userEmail == null || userEmail.isBlank()) {
            return;
        }
        if (!matchesOrderEmail(order, userEmail)) {
            return;
        }
        User viewer = userRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (viewer == null) {
            return;
        }
        order.setUser(viewer);
        orderRepository.save(order);
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

        order.getStatusHistory().size();

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
        if (matchesOrderEmail(order, emailHint)) {
            return;
        }
        if (userEmail != null) {
            if (matchesOrderEmail(order, userEmail)) {
                return;
            }
            User viewer = userRepository.findByEmail(userEmail.trim()).orElse(null);
            if (viewer != null) {
                if (order.getUser() != null && viewer.getId().equals(order.getUser().getId())) {
                    return;
                }
                Order linked = orderRepository.findByOrderNumberWithUser(order.getOrderNumber()).orElse(null);
                if (linked != null && linked.getUser() != null && viewer.getId().equals(linked.getUser().getId())) {
                    return;
                }
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this order");
        }
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Login or provide the order email to view this order");
    }

    private boolean matchesOrderEmail(Order order, String email) {
        String normalizedOrderEmail = normalizeEmail(order.getEmail());
        String normalizedInput = normalizeEmail(email);
        return normalizedInput != null
                && normalizedOrderEmail != null
                && normalizedInput.equals(normalizedOrderEmail);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
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
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be marked paid from status " + order.getStatus());
        }
        if (order.getStatus() == OrderStatus.FAILED) {
            reopenForPaymentRetry(orderNumber);
            order = orderRepository.findByOrderNumberWithItems(orderNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        }

        order.setStatus(OrderStatus.PAID);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PAID);
        history.setNote(note != null ? note : "Payment succeeded");
        order.getStatusHistory().add(history);
        Order saved = orderRepository.save(order);
        emailService.sendOrderConfirmationEmail(saved);
        return toDto(saved);
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

    /**
     * Re-reserve stock and move a failed order back to awaiting payment so the customer can retry checkout.
     */
    @Transactional
    public OrderDto reopenForPaymentRetry(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already paid");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            return toDto(order);
        }
        if (order.getStatus() != OrderStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order cannot be reopened for payment (status=" + order.getStatus() + ")");
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Product no longer available: " + item.getProductName()));

            if (product.getStockQuantity() < item.getQuantity()) {
                String message = product.getStockQuantity() <= 0
                        ? product.getName() + " is out of stock"
                        : "Only " + product.getStockQuantity() + " left in stock for " + product.getName();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.PENDING_PAYMENT);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PENDING_PAYMENT);
        history.setNote("Payment retry — stock re-reserved");
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
        applyShipping(dto, order);
        dto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO) : null);
        dto.setItems(order.getItems().stream().map(item -> toItemDto(order, item)).toList());
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
        applyShipping(dto, order);
        dto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO) : null);
        dto.setItems(order.getItems().stream().map(item -> toItemDto(order, item)).toList());
        dto.setStatusHistory(List.of());
        return dto;
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

    private OrderItemDto toItemDto(Order order, OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
        dto.setProductId(productId);
        dto.setProductName(item.getProductName());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        applyReviewState(dto, order, productId);
        return dto;
    }

    private void applyReviewState(OrderItemDto dto, Order order, Long productId) {
        dto.setCanReview(false);
        if (productId == null || order.getUser() == null || order.getStatus() == null) {
            return;
        }
        if (!REVIEWABLE_STATUSES.contains(order.getStatus())) {
            return;
        }
        reviewRepository.findByUserIdAndProductId(order.getUser().getId(), productId)
                .ifPresentOrElse(
                        review -> dto.setReviewStatus(review.getStatus().name()),
                        () -> dto.setCanReview(true));
    }

    private OrderStatusHistoryDto toHistoryDto(OrderStatusHistory history) {
        OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
        dto.setStatus(history.getStatus().name());
        dto.setNote(history.getNote());
        dto.setCreatedAt(history.getCreatedAt() != null ? history.getCreatedAt().format(ISO) : null);
        return dto;
    }

    private static String blankToNull(String value) {
        return CheckoutFieldValidator.normalize(value);
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
