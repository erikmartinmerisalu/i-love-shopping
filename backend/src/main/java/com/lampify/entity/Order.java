package com.lampify.entity;

import com.lampify.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "full_name", nullable = false, length = 512)
    private String fullName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "address_line1", nullable = false, length = 1024)
    private String addressLine1;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "address_line2", length = 1024)
    private String addressLine2;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String city;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "postal_code", nullable = false, length = 512)
    private String postalCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String country;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
