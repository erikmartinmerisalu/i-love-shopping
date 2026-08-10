package com.lampify.service;

import com.lampify.dto.CheckoutRequest;
import com.lampify.dto.OrderDto;
import com.lampify.dto.ValidationErrorResponse;
import com.lampify.entity.Cart;
import com.lampify.entity.CartItem;
import com.lampify.entity.Order;
import com.lampify.entity.OrderItem;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.PaymentMethod;
import com.lampify.entity.Product;
import com.lampify.entity.User;
import com.lampify.repository.CartRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private Product productA;
    private Product productB;
    private Cart guestCart;

    @BeforeEach
    void setUp() {
        productA = product(1L, "Lamp A", "20.00", 10);
        productB = product(2L, "Lamp B", "15.50", 10);

        guestCart = new Cart();
        guestCart.setId(5L);
        guestCart.setGuestToken("guest-abc");
        guestCart.setItems(new ArrayList<>());

        CartItem itemA = new CartItem();
        itemA.setProduct(productA);
        itemA.setQuantity(2);
        itemA.setCart(guestCart);

        CartItem itemB = new CartItem();
        itemB.setProduct(productB);
        itemB.setQuantity(1);
        itemB.setCart(guestCart);

        guestCart.getItems().add(itemA);
        guestCart.getItems().add(itemB);
    }

    @Test
    void validateCheckoutRejectsInvalidEmailAndPhone() {
        CheckoutRequest request = validCheckout();
        request.setEmail("not-an-email");
        request.setPhone("abc");

        ValidationErrorResponse errors = orderService.validateCheckout(request);

        assertNotNull(errors);
        assertEquals("Invalid email format", errors.getFieldErrors().get("email"));
        assertEquals("Invalid phone format", errors.getFieldErrors().get("phone"));
    }

    @Test
    void validateCheckoutAcceptsValidPayload() {
        assertNull(orderService.validateCheckout(validCheckout()));
    }

    @Test
    void placeOrderCalculatesTotalFromLineItems() {
        when(cartRepository.findByGuestTokenWithItems("guest-abc")).thenReturn(Optional.of(guestCart));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(productB));
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        OrderDto dto = orderService.placeOrder(null, "guest-abc", validCheckout());

        // 2 * 20.00 + 1 * 15.50 = 55.50
        assertEquals(new BigDecimal("55.50"), dto.getTotalAmount());
        assertEquals(2, dto.getItems().size());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), dto.getStatus());
        assertEquals(8, productA.getStockQuantity());
        assertEquals(9, productB.getStockQuantity());
        verify(emailService, never()).sendOrderConfirmationEmail(any(Order.class));
        verify(cartRepository).save(guestCart);
        assertTrue(guestCart.getItems().isEmpty());
    }

    @Test
    void markPaymentSucceededSendsConfirmationEmail() {
        Order order = new Order();
        order.setId(7L);
        order.setOrderNumber("EV-TESTORDER1");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setEmail("buyer@example.com");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumberWithItems("EV-TESTORDER1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.markPaymentSucceeded("EV-TESTORDER1", "Sandbox payment");

        assertEquals(OrderStatus.PAID.name(), result.getStatus());
        verify(emailService).sendOrderConfirmationEmail(order);
    }

    @Test
    void cancelOrderAllowsLoggedInUserWhenCheckoutEmailMatches() {
        Order order = new Order();
        order.setId(7L);
        order.setOrderNumber("EV-TESTORDER1");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setEmail("buyer@example.com");
        order.setPaymentMethod(PaymentMethod.PAYPAL);
        order.setItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumberWithItems("EV-TESTORDER1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.cancelOrder("EV-TESTORDER1", "buyer@example.com", null);

        assertEquals(OrderStatus.CANCELLED.name(), result.getStatus());
    }

    @Test
    void cancelOrderAllowsEmailHintEvenWhenLoggedIn() {
        Order order = new Order();
        order.setId(8L);
        order.setOrderNumber("EV-TESTORDER2");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setEmail("buyer@example.com");
        order.setPaymentMethod(PaymentMethod.PAYPAL);
        order.setItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumberWithItems("EV-TESTORDER2")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.cancelOrder("EV-TESTORDER2", "other@example.com", "buyer@example.com");

        assertEquals(OrderStatus.CANCELLED.name(), result.getStatus());
    }

    @Test
    void cancelOrderRestoresStockForPendingPayment() {
        Order order = new Order();
        order.setId(7L);
        order.setOrderNumber("EV-TESTORDER1");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setEmail("buyer@example.com");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());

        productA.setStockQuantity(8);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(productA);
        orderItem.setProductName(productA.getName());
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(productA.getPrice());
        orderItem.setLineTotal(new BigDecimal("40.00"));
        order.getItems().add(orderItem);

        when(orderRepository.findByOrderNumberWithItems("EV-TESTORDER1")).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(productA));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.cancelOrder("EV-TESTORDER1", null, "buyer@example.com");

        assertEquals(OrderStatus.CANCELLED.name(), result.getStatus());
        assertEquals(10, productA.getStockQuantity());
    }

    @Test
    void cancelOrderRejectsPaidOrders() {
        Order order = new Order();
        order.setOrderNumber("EV-PAIDORDER01");
        order.setStatus(OrderStatus.PAID);
        order.setEmail("buyer@example.com");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumberWithItems("EV-PAIDORDER01")).thenReturn(Optional.of(order));

        assertThrows(
                ResponseStatusException.class,
                () -> orderService.cancelOrder("EV-PAIDORDER01", null, "buyer@example.com"));
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    private CheckoutRequest validCheckout() {
        CheckoutRequest request = new CheckoutRequest();
        request.setFullName("Test Buyer");
        request.setEmail("buyer@example.com");
        request.setPhone("+37255555555");
        request.setAddressLine1("Narva mnt 1");
        request.setCity("Tallinn");
        request.setPostalCode("10111");
        request.setCountry("Estonia");
        request.setPaymentMethod("CARD");
        return request;
    }

    private Product product(Long id, String name, String price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setImages(new ArrayList<>());
        return product;
    }
}
