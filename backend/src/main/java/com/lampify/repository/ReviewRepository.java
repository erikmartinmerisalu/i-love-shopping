package com.lampify.repository;

import com.lampify.entity.Review;
import com.lampify.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status);

    List<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status);

    @Query("""
            SELECT DISTINCT r FROM Review r
            JOIN FETCH r.user
            JOIN FETCH r.product
            WHERE r.status = :status
            ORDER BY r.createdAt ASC
            """)
    List<Review> findByStatusWithUserAndProductOrderByCreatedAtAsc(@Param("status") ReviewStatus status);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0) FROM Review r
            WHERE r.product.id = :productId AND r.status = :status
            """)
    double averageRatingForProduct(@Param("productId") Long productId, @Param("status") ReviewStatus status);
}
