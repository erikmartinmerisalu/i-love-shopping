package com.lampify.support;

import com.lampify.repository.CartRepository;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.DeliveryOptionRepository;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.PaymentTransactionRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.RefreshTokenRepository;
import com.lampify.repository.ReviewHelpfulVoteRepository;
import com.lampify.repository.ReviewRepository;
import org.springframework.stereotype.Component;

/**
 * Clears commerce data in FK-safe order so integration tests can re-seed products.
 */
@Component
public class TestDatabaseCleaner {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public TestDatabaseCleaner(
            PaymentTransactionRepository paymentTransactionRepository,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            ReviewHelpfulVoteRepository reviewHelpfulVoteRepository,
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            DeliveryOptionRepository deliveryOptionRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.reviewHelpfulVoteRepository = reviewHelpfulVoteRepository;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.deliveryOptionRepository = deliveryOptionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void resetCatalogData() {
        reviewHelpfulVoteRepository.deleteAll();
        reviewRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        deliveryOptionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }
}
