package com.lampify.service;

import com.lampify.dto.AddToCartRequest;
import com.lampify.dto.UpdateCartItemRequest;
import com.lampify.entity.Cart;
import com.lampify.entity.CartItem;
import com.lampify.entity.Product;
import com.lampify.repository.CartItemRepository;
import com.lampify.repository.CartRepository;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private Product product;
    private Cart guestCart;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Lamp");
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(5);
        product.setImages(new ArrayList<>());

        guestCart = new Cart();
        guestCart.setId(10L);
        guestCart.setGuestToken("guest-token");
        guestCart.setItems(new ArrayList<>());
    }

    @Test
    void addItemCalculatesLineAndCartTotals() {
        when(cartRepository.findByGuestTokenWithItems("guest-token")).thenAnswer(invocation -> Optional.of(guestCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        CartService.CartResult result = cartService.addItem(null, "guest-token", request);

        assertEquals(1, result.cart().getItems().size());
        assertEquals(2, result.cart().getTotalItems());
        assertEquals(new BigDecimal("50.00"), result.cart().getTotalPrice());
        assertEquals(new BigDecimal("50.00"), result.cart().getItems().get(0).getLineTotal());
    }

    @Test
    void addItemRejectsOutOfStock() {
        product.setStockQuantity(0);
        when(cartRepository.findByGuestTokenWithItems("guest-token")).thenReturn(Optional.of(guestCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItem(null, "guest-token", request));
        assertTrue(ex.getReason().toLowerCase().contains("out of stock"));
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        product.setStockQuantity(2);
        when(cartRepository.findByGuestTokenWithItems("guest-token")).thenReturn(Optional.of(guestCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItem(null, "guest-token", request));
        assertTrue(ex.getReason().contains("Only 2 left"));
    }

    @Test
    void updateQuantityToZeroRemovesItemAndZerosTotal() {
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setCart(guestCart);
        existing.setProduct(product);
        existing.setQuantity(2);
        guestCart.getItems().add(existing);

        when(cartRepository.findByGuestTokenWithItems("guest-token")).thenAnswer(invocation -> Optional.of(guestCart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.of(existing));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(0);

        CartService.CartResult result = cartService.updateItem(null, "guest-token", 1L, request);

        assertEquals(0, result.cart().getTotalItems());
        assertEquals(BigDecimal.ZERO, result.cart().getTotalPrice());
        verify(cartItemRepository).delete(existing);
    }
}
