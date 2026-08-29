package com.lampify.service;

import com.lampify.dto.SubmitReviewRequest;
import com.lampify.entity.*;
import com.lampify.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewHelpfulVoteRepository helpfulVoteRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        customer = new User();
        customer.setId(10L);
        customer.setEmail("buyer@example.com");

        product = new Product();
        product.setId(1L);
        product.setRating(new BigDecimal("4.00"));
        product.setReviewCount(0);

        order = new Order();
        order.setId(100L);
        order.setOrderNumber("ORD-TEST");
        order.setUser(customer);
    }

    @Test
    void submitReviewRequiresPurchase() {
        authenticate(customer.getEmail());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(10L, 1L)).thenReturn(false);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.userPurchasedProduct(eq(10L), eq(1L), eq(100L), anyCollection())).thenReturn(false);

        SubmitReviewRequest request = new SubmitReviewRequest(1L, 100L, 5, "Great lamp, very bright and stylish.");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> reviewService.submitReview(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void submitReviewCreatesPendingReview() {
        authenticate(customer.getEmail());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(10L, 1L)).thenReturn(false);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.userPurchasedProduct(eq(10L), eq(1L), eq(100L), anyCollection())).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        SubmitReviewRequest request = new SubmitReviewRequest(1L, 100L, 5, "Great lamp, very bright and stylish.");
        var dto = reviewService.submitReview(request);

        assertEquals(50L, dto.getId());
        assertEquals(5, dto.getRating());
        assertEquals("PENDING", dto.getStatus());
        verify(reviewRepository).save(argThat(review -> review.getStatus() == ReviewStatus.PENDING));
    }

    @Test
    void approveReviewRecalculatesProductRating() {
        Review review = new Review();
        review.setId(50L);
        review.setProduct(product);
        review.setUser(customer);
        review.setOrder(order);
        review.setRating(4);
        review.setBody("Nice product overall.");
        review.setStatus(ReviewStatus.PENDING);

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.APPROVED)).thenReturn(1L);
        when(reviewRepository.averageRatingForProduct(1L, ReviewStatus.APPROVED)).thenReturn(4.0);
        when(helpfulVoteRepository.countByReviewIds(anyList())).thenReturn(List.of());

        reviewService.approveReview(50L);

        verify(productRepository).save(argThat(p -> p.getReviewCount() == 1 && p.getRating().compareTo(new BigDecimal("4.00")) == 0));
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}
