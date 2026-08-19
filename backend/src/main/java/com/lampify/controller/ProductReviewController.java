package com.lampify.controller;

import com.lampify.dto.ReviewDto;
import com.lampify.dto.ReviewListResponse;
import com.lampify.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductReviewController {

    private final ReviewService reviewService;

    public ProductReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ReviewListResponse> listReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "helpful") String sort) {
        return ResponseEntity.ok(reviewService.listProductReviews(productId, sort));
    }
}
