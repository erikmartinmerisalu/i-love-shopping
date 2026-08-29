package com.lampify.repository;

import com.lampify.entity.Order;
import com.lampify.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.user WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithUser(@Param("orderNumber") String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);

    List<Order> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, OrderStatus status);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByDeliveryOption_Id(Long deliveryOptionId);

    @Query("""
            SELECT COUNT(o) > 0 FROM Order o JOIN o.items i
            WHERE o.id = :orderId AND o.user.id = :userId
            AND o.status IN :statuses AND i.product.id = :productId
            """)
    boolean userPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("orderId") Long orderId,
            @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            SELECT DISTINCT o FROM Order o JOIN FETCH o.items i
            WHERE o.user.id = :userId AND i.product.id = :productId
            AND o.status IN :statuses
            ORDER BY o.createdAt DESC
            """)
    List<Order> findEligibleOrdersForReview(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("statuses") Collection<OrderStatus> statuses);
}
