package com.lampify.repository;

import com.lampify.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.images
            WHERE p.stockQuantity > 0
              AND p.category.id IN :categoryIds
              AND p.id NOT IN :excludeIds
            ORDER BY p.rating DESC
            """)
    List<Product> findRecommendationsByCategoryIds(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("excludeIds") Collection<Long> excludeIds,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.images
            WHERE p.stockQuantity > 0
              AND p.id NOT IN :excludeIds
            ORDER BY p.rating DESC
            """)
    List<Product> findPopularExcluding(
            @Param("excludeIds") Collection<Long> excludeIds,
            Pageable pageable);
}
