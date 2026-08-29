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

    @Column(name = "thumb_path", nullable = false, length = 500)
    private String thumbPath;

    @Column(name = "medium_path", nullable = false, length = 500)
    private String mediumPath;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryImage = false;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void fillVariantPaths() {
        if (thumbPath == null || thumbPath.isBlank()) {
            thumbPath = urlPath;
        }
        if (mediumPath == null || mediumPath.isBlank()) {
            mediumPath = urlPath;
        }
    }

    public String effectiveThumbPath() {
        return (thumbPath == null || thumbPath.isBlank()) ? urlPath : thumbPath;
    }

    public String effectiveMediumPath() {
        return (mediumPath == null || mediumPath.isBlank()) ? urlPath : mediumPath;
    }
}
