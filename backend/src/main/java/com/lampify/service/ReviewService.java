package com.lampify.service;

import com.lampify.dto.*;
import com.lampify.entity.*;
import com.lampify.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final List<OrderStatus> ELIGIBLE_ORDER_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.FULFILLED);

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewHelpfulVoteRepository helpfulVoteRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.reviewRepository = reviewRepository;
        this.helpfulVoteRepository = helpfulVoteRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listProductReviews(Long productId, String sort) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        List<Review> reviews = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, ReviewStatus.APPROVED);
        Map<Long, Long> helpfulCounts = loadHelpfulCounts(reviews);
        Long currentUserId = getCurrentUserIdOrNull();

        Comparator<Review> comparator = "recent".equalsIgnoreCase(sort)
                ? Comparator.comparing(Review::getCreatedAt).reversed()
                : Comparator.<Review>comparingLong(r -> helpfulCounts.getOrDefault(r.getId(), 0L)).reversed()
                        .thenComparing(Review::getCreatedAt, Comparator.reverseOrder());

        List<ReviewDto> dtos = reviews.stream()
                .sorted(comparator)
                .map(review -> toReviewDto(review, helpfulCounts, currentUserId, false))
                .toList();

        ReviewListResponse response = new ReviewListResponse();
        response.setReviews(dtos);
        response.setTotalElements(dtos.size());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ReviewEligibleOrderDto> listEligibleOrders(Long productId) {
        User user = requireAuthenticatedUser();
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return List.of();
        }

        return orderRepository.findEligibleOrdersForReview(
                        user.getId(), productId, ELIGIBLE_ORDER_STATUSES)
                .stream()
                .map(order -> new ReviewEligibleOrderDto(order.getId(), order.getOrderNumber()))
                .toList();
    }

    @Transactional
    public ReviewDto submitReview(SubmitReviewRequest request) {
        User user = requireAuthenticatedUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this product");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        attachGuestOrderToUser(order, user);

        if (!orderRepository.userPurchasedProduct(
                user.getId(), product.getId(), request.getOrderId(), ELIGIBLE_ORDER_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this product before reviewing");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setOrder(order);
        review.setRating(request.getRating());
        review.setBody(request.getBody().trim());
        review.setStatus(ReviewStatus.PENDING);

        review = reviewRepository.save(review);
        return toReviewDto(review, Map.of(), user.getId(), true);
    }

    @Transactional
    public HelpfulVoteResponse toggleHelpfulVote(Long reviewId) {
        User user = requireAuthenticatedUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved reviews can receive votes");
        }

        Optional<ReviewHelpfulVote> existing = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, user.getId());
        if (existing.isPresent()) {
            helpfulVoteRepository.delete(existing.get());
            return new HelpfulVoteResponse(helpfulVoteRepository.countByReviewId(reviewId), false);
        }

        ReviewHelpfulVote vote = new ReviewHelpfulVote();
        vote.setReview(review);
        vote.setUser(user);
        helpfulVoteRepository.save(vote);
        return new HelpfulVoteResponse(helpfulVoteRepository.countByReviewId(reviewId), true);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> listReviewsForModeration(String status) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        ReviewStatus reviewStatus = parseStatus(status, ReviewStatus.PENDING);
        List<Review> reviews = reviewRepository.findByStatusWithUserAndProductOrderByCreatedAtAsc(reviewStatus);
        Map<Long, Long> helpfulCounts = loadHelpfulCounts(reviews);
        return reviews.stream()
                .map(review -> toReviewDto(review, helpfulCounts, null, true))
                .toList();
    }

    @Transactional
    public ReviewDto approveReview(Long reviewId) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
        recalculateProductRating(review.getProduct().getId());
        return toReviewDto(review, loadHelpfulCounts(List.of(review)), null, true);
    }

    @Transactional
    public ReviewDto rejectReview(Long reviewId) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        boolean wasApproved = review.getStatus() == ReviewStatus.APPROVED;
        review.setStatus(ReviewStatus.REJECTED);
        reviewRepository.save(review);
        if (wasApproved) {
            recalculateProductRating(review.getProduct().getId());
        }
        return toReviewDto(review, loadHelpfulCounts(List.of(review)), null, true);
    }

    void recalculateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        long count = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        product.setReviewCount((int) count);
        if (count == 0) {
            productRepository.save(product);
            return;
        }
        double average = reviewRepository.averageRatingForProduct(productId, ReviewStatus.APPROVED);
        product.setRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        productRepository.save(product);
    }

    private Map<Long, Long> loadHelpfulCounts(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        return helpfulVoteRepository.countByReviewIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
    }

    private ReviewDto toReviewDto(
            Review review,
            Map<Long, Long> helpfulCounts,
            Long currentUserId,
            boolean includeStatus) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setProductName(review.getProduct().getName());
        dto.setRating(review.getRating());
        dto.setBody(review.getBody());
        dto.setAuthorName(includeStatus ? adminAuthorLabel(review.getUser()) : maskAuthorName(review.getUser()));
        if (includeStatus) {
            dto.setAuthorUsername(review.getUser().getUsername());
        }
        dto.setCreatedAt(review.getCreatedAt());
        dto.setHelpfulCount(helpfulCounts.getOrDefault(review.getId(), 0L));
        if (currentUserId != null) {
            dto.setHelpfulByCurrentUser(
                    helpfulVoteRepository.findByReviewIdAndUserId(review.getId(), currentUserId).isPresent());
        }
        if (includeStatus) {
            dto.setStatus(review.getStatus().name());
        }
        return dto;
    }

    private String adminAuthorLabel(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private String maskAuthorName(User user) {
        String email = user.getEmail();
        int at = email.indexOf('@');
        if (at <= 1) {
            return "Verified buyer";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private void attachGuestOrderToUser(Order order, User user) {
        if (order.getUser() != null || user.getEmail() == null) {
            return;
        }
        String orderEmail = order.getEmail();
        if (orderEmail != null && orderEmail.trim().equalsIgnoreCase(user.getEmail().trim())) {
            order.setUser(user);
            orderRepository.save(order);
        }
    }

    private User requireAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Long getCurrentUserIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).map(User::getId).orElse(null);
    }

    private ReviewStatus parseStatus(String status, ReviewStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review status");
        }
    }
}
