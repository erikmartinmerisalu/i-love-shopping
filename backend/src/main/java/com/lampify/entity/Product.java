package com.lampify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity = 0;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(precision = 8, scale = 3)
    private BigDecimal weightLb;

    @Column(precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(precision = 8, scale = 2)
    private BigDecimal lengthIn;

    @Column(precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(precision = 8, scale = 2)
    private BigDecimal widthIn;

    @Column(precision = 8, scale = 2)
    private BigDecimal heightCm;

    @Column(precision = 8, scale = 2)
    private BigDecimal heightIn;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();
}
