package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long productId;
    private int rating;
    private String body;
    private String authorName;
    private LocalDateTime createdAt;
    private long helpfulCount;
    private boolean helpfulByCurrentUser;
    private String status;
}
