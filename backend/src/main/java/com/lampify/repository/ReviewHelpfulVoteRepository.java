package com.lampify.repository;

import com.lampify.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    @Query("""
            SELECT v.review.id, COUNT(v) FROM ReviewHelpfulVote v
            WHERE v.review.id IN :reviewIds
            GROUP BY v.review.id
            """)
    List<Object[]> countByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);
}
