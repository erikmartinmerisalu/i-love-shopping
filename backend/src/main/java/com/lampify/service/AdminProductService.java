package com.lampify.service;

import com.lampify.dto.AdminProductRequest;
import com.lampify.dto.BulkUploadResult;
import com.lampify.dto.ProductDetailDto;
import com.lampify.dto.ProductDto;
import com.lampify.dto.ProductListResponse;
import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductService productService,
            AdminAuthorizationService adminAuthorizationService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productService = productService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public ProductListResponse listProducts(int page, int size) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Product> products = productRepository.findAllByOrderByNameAsc(PageRequest.of(safePage, safeSize));

        ProductListResponse response = new ProductListResponse();
        response.setProducts(products.getContent().stream().map(this::toAdminProductDto).toList());
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalElements(products.getTotalElements());
        response.setTotalPages(products.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProduct(Long id) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        return productService.getProductById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public ProductDetailDto createProduct(AdminProductRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Product product = applyRequest(new Product(), request);
        Product saved = productRepository.save(product);
        return productService.getProductById(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Product not found after save"));
    }

    @Transactional
    public ProductDetailDto updateProduct(Long id, AdminProductRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        applyRequest(product, request);
        productRepository.save(product);
        return productService.getProductById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public void deleteProduct(Long id) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        productRepository.delete(product);
    }

    @Transactional
    public ProductDto uploadImage(Long productId, MultipartFile file, boolean makePrimary) throws IOException {
        adminAuthorizationService.requireAdminWithTwoFactor();
        return productService.uploadProductImage(productId, file, makePrimary);
    }

    @Transactional
    public BulkUploadResult bulkUpload(MultipartFile file) throws IOException {
        adminAuthorizationService.requireAdminWithTwoFactor();
        BulkUploadResult result = new BulkUploadResult();
        if (file == null || file.isEmpty()) {
            result.getErrors().add("Upload file is empty");
            result.setSkipped(1);
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                result.getErrors().add("CSV header row is missing");
                return result;
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                try {
                    upsertCsvRow(line, result, rowNumber);
                } catch (Exception ex) {
                    result.setSkipped(result.getSkipped() + 1);
                    result.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                }
            }
        }

        return result;
    }

    private void upsertCsvRow(String line, BulkUploadResult result, int rowNumber) {
        String[] parts = line.split(",", -1);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Expected at least 8 columns");
        }

        String name = parts[0].trim();
        String description = parts[1].trim();
        BigDecimal price = new BigDecimal(parts[2].trim());
        int stock = Integer.parseInt(parts[3].trim());
        String brand = parts[4].trim();
        String categorySlug = parts[5].trim().toLowerCase(Locale.ROOT);
        String sku = parts[6].trim();
        boolean active = true;
        if (parts.length > 7 && !parts[7].isBlank()) {
            active = Boolean.parseBoolean(parts[7].trim());
        }
        if (parts.length > 8 && !parts[8].isBlank()) {
            // featured column optional
        }

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category slug: " + categorySlug));

        Product product = (sku.isBlank() ? null : productRepository.findBySku(sku).orElse(null));
        boolean created = product == null;
        if (product == null) {
            product = new Product();
        }

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setBrand(brand);
        product.setCategory(category);
        product.setSku(sku.isBlank() ? null : sku);
        product.setActive(active);
        productRepository.save(product);

        if (created) {
            result.setCreated(result.getCreated() + 1);
        } else {
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    private Product applyRequest(Product product, AdminProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setBrand(request.getBrand().trim());
        product.setCategory(category);
        product.setSku(trimToNull(request.getSku()));
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());
        product.setWeightKg(request.getWeightKg());
        product.setWeightLb(request.getWeightLb());
        product.setLengthCm(request.getLengthCm());
        product.setLengthIn(request.getLengthIn());
        product.setWidthCm(request.getWidthCm());
        product.setWidthIn(request.getWidthIn());
        product.setHeightCm(request.getHeightCm());
        product.setHeightIn(request.getHeightIn());
        return product;
    }

    private ProductDto toAdminProductDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setBrand(product.getBrand());
        dto.setRating(product.getRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setCategory(product.getCategory().getName());
        dto.setCategorySlug(product.getCategory().getSlug());
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
