package com.lampify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String urlPath;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryImage = false;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
