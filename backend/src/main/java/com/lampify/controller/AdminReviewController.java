package com.lampify.controller;

import com.lampify.dto.ReviewDto;
import com.lampify.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<ReviewDto>> listReviews(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(reviewService.listReviewsForModeration(status));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReviewDto> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReviewDto> rejectReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }
}
