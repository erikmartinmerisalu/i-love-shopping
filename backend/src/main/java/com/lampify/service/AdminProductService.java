package com.lampify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lampify.dto.AdminProductRequest;
import com.lampify.dto.BulkProductRow;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ObjectMapper objectMapper;

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductService productService,
            AdminAuthorizationService adminAuthorizationService,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productService = productService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.objectMapper = objectMapper;
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

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String body = new String(file.getBytes(), StandardCharsets.UTF_8).strip();
        boolean json = originalName.endsWith(".json")
                || contentType.contains("json")
                || body.startsWith("[");

        if (json) {
            importJson(body, result);
        } else {
            importCsv(body, result);
        }
        return result;
    }

    private void importCsv(String body, BulkUploadResult result) {
        String[] lines = body.split("\\R");
        if (lines.length == 0 || lines[0].isBlank()) {
            result.getErrors().add("CSV header row is missing");
            return;
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            int rowNumber = i + 1;
            try {
                upsertRow(fromCsvLine(line), result);
            } catch (Exception ex) {
                result.setSkipped(result.getSkipped() + 1);
                result.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
            }
        }
    }

    private void importJson(String body, BulkUploadResult result) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root == null || !root.isArray()) {
            result.getErrors().add("JSON upload must be an array of product objects");
            result.setSkipped(1);
            return;
        }

        int index = 0;
        for (JsonNode node : root) {
            index++;
            try {
                BulkProductRow row = objectMapper.treeToValue(node, BulkProductRow.class);
                upsertRow(row, result);
            } catch (Exception ex) {
                result.setSkipped(result.getSkipped() + 1);
                result.getErrors().add("Item " + index + ": " + ex.getMessage());
            }
        }
    }

    private BulkProductRow fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Expected at least 8 columns");
        }
        BulkProductRow row = new BulkProductRow();
        row.setName(parts[0].trim());
        row.setDescription(parts[1].trim());
        row.setPrice(new BigDecimal(parts[2].trim()));
        row.setStockQuantity(Integer.parseInt(parts[3].trim()));
        row.setBrand(parts[4].trim());
        row.setCategorySlug(parts[5].trim());
        row.setSku(parts[6].trim());
        if (parts.length > 7 && !parts[7].isBlank()) {
            row.setActive(Boolean.parseBoolean(parts[7].trim()));
        }
        if (parts.length > 8 && !parts[8].isBlank()) {
            row.setFeatured(Boolean.parseBoolean(parts[8].trim()));
        }
        return row;
    }

    private void upsertRow(BulkProductRow row, BulkUploadResult result) {
        if (row.getName() == null || row.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (row.getDescription() == null || row.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (row.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (row.getStockQuantity() == null) {
            throw new IllegalArgumentException("Stock quantity is required");
        }
        if (row.getBrand() == null || row.getBrand().isBlank()) {
            throw new IllegalArgumentException("Brand is required");
        }
        if (row.getCategorySlug() == null || row.getCategorySlug().isBlank()) {
            throw new IllegalArgumentException("Category slug is required");
        }

        String categorySlug = row.getCategorySlug().trim().toLowerCase(Locale.ROOT);
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category slug: " + categorySlug));

        String sku = row.getSku() == null ? "" : row.getSku().trim();
        Product product = sku.isBlank() ? null : productRepository.findBySku(sku).orElse(null);
        boolean created = product == null;
        if (product == null) {
            product = new Product();
        }

        product.setName(row.getName().trim());
        product.setDescription(row.getDescription().trim());
        product.setPrice(row.getPrice());
        product.setStockQuantity(row.getStockQuantity());
        product.setBrand(row.getBrand().trim());
        product.setCategory(category);
        product.setSku(sku.isBlank() ? null : sku);
        product.setActive(row.getActive() == null || row.getActive());
        product.setFeatured(Boolean.TRUE.equals(row.getFeatured()));
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
