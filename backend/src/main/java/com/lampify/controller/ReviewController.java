package com.lampify.controller;

import com.lampify.dto.*;
import com.lampify.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> submitReview(@Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.ok(reviewService.submitReview(request));
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<HelpfulVoteResponse> toggleHelpful(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.toggleHelpfulVote(id));
    }

    @GetMapping("/eligible")
    public ResponseEntity<List<ReviewEligibleOrderDto>> listEligibleOrders(@RequestParam Long productId) {
        return ResponseEntity.ok(reviewService.listEligibleOrders(productId));
    }
}
