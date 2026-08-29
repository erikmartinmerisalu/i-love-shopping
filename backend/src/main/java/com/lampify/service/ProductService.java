package com.lampify.service;

import com.lampify.dto.*;
import com.lampify.entity.Category;
import com.lampify.entity.Product;
import com.lampify.entity.ProductImage;
import com.lampify.repository.CategoryRepository;
import com.lampify.repository.ProductImageRepository;
import com.lampify.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository,
            FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.fileStorageService = fileStorageService;
    }

    public ProductListResponse searchProducts(ProductSearchCriteria criteria) {
        int size = Math.min(Math.max(criteria.getSize(), 1), 100);
        int page = Math.max(criteria.getPage(), 0);
        criteria.setSize(size);
        criteria.setPage(page);

        List<Product> products = productRepository.searchProducts(criteria);
        long total = productRepository.countProducts(criteria);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        ProductListResponse response = new ProductListResponse();
        response.setProducts(products.stream().map(this::toProductDto).toList());
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(total);
        response.setTotalPages(totalPages);
        response.setFacets(buildFacets());
        return response;
    }

    public Optional<ProductDetailDto> getProductById(Long id) {
        return productRepository.findById(id).map(this::toProductDetailDto);
    }

    public HomeDto getHomeData() {
        Pageable featuredPage = PageRequest.of(0, 8);
        List<ProductDto> featured = productRepository.findFeaturedProducts(featuredPage).stream()
                .map(this::toProductDto)
                .toList();

        List<Category> categories = categoryRepository.findAll();
        Pageable collectionPage = PageRequest.of(0, 4);
        List<CategoryCollectionDto> collections = categories.stream()
                .map(category -> {
                    List<ProductDto> products = productRepository.findTopByCategoryId(category.getId(), collectionPage)
                            .stream()
                            .map(this::toProductDto)
                            .toList();
                    CategoryCollectionDto collection = new CategoryCollectionDto();
                    collection.setCategory(toCategoryDto(category));
                    collection.setProducts(products);
                    return collection;
                })
                .filter(collection -> !collection.getProducts().isEmpty())
                .toList();

        HomeDto home = new HomeDto();
        home.setFeaturedProducts(featured);
        home.setCollections(collections);
        return home;
    }

    public List<ProductSuggestionDto> suggestProducts(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), 12);
        Pageable pageable = PageRequest.of(0, size);
        return productRepository.findSuggestions(query.trim(), pageable).stream()
                .map(this::toSuggestionDto)
                .toList();
    }

    public List<ProductDto> getRelatedProducts(Long productId, int limit) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return List.of();
        }
        Product product = productOpt.get();
        int size = Math.min(Math.max(limit, 1), 12);
        Pageable pageable = PageRequest.of(0, size);
        List<Product> related = productRepository.findRelatedInCategory(
                product.getCategory().getId(),
                product.getId(),
                pageable);
        if (related.isEmpty()) {
            related = productRepository.findPopularExcluding(List.of(product.getId()), pageable);
        }
        return related.stream().map(this::toProductDto).toList();
    }

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional
    public ProductDto uploadProductImage(Long productId, MultipartFile file, boolean makePrimary) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        FileStorageService.StoredFile storedFile = fileStorageService.storeProductImage(productId, file);

        if (makePrimary) {
            product.getImages().forEach(image -> image.setPrimaryImage(false));
        }

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setFileName(storedFile.fileName());
        image.setUrlPath(storedFile.urlPath());
        image.setThumbPath(storedFile.thumbPath());
        image.setMediumPath(storedFile.mediumPath());
        image.setPrimaryImage(makePrimary || product.getImages().isEmpty());
        image.setSortOrder(product.getImages().size());
        product.getImages().add(image);
        productImageRepository.save(image);

        return toProductDto(product);
    }

    private ProductListResponse.FacetsDto buildFacets() {
        ProductListResponse.FacetsDto facets = new ProductListResponse.FacetsDto();
        facets.setCategories(categoryRepository.findAll().stream().map(this::toCategoryDto).toList());
        facets.setBrands(productRepository.findDistinctBrands());
        facets.setMinPrice(productRepository.findMinPrice());
        facets.setMaxPrice(productRepository.findMaxPrice());
        return facets;
    }

    private ProductDto toProductDto(Product product) {
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
        dto.setPrimaryImageUrl(resolvePrimaryImage(product));
        dto.setWeightKg(product.getWeightKg());
        dto.setWeightLb(product.getWeightLb());
        dto.setLengthCm(product.getLengthCm());
        dto.setLengthIn(product.getLengthIn());
        dto.setWidthCm(product.getWidthCm());
        dto.setWidthIn(product.getWidthIn());
        dto.setHeightCm(product.getHeightCm());
        dto.setHeightIn(product.getHeightIn());
        return dto;
    }

    private ProductDetailDto toProductDetailDto(Product product) {
        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setBrand(product.getBrand());
        dto.setRating(product.getRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setCategory(toCategoryDto(product.getCategory()));
        dto.setImageUrls(product.getImages().stream().map(ProductImage::getUrlPath).toList());
        dto.setThumbnailUrls(product.getImages().stream().map(ProductImage::effectiveThumbPath).toList());
        dto.setSku(product.getSku());
        dto.setActive(product.isActive());
        dto.setFeatured(product.isFeatured());

        ProductDetailDto.DimensionsDto dimensions = new ProductDetailDto.DimensionsDto();
        dimensions.setWeightKg(product.getWeightKg());
        dimensions.setWeightLb(product.getWeightLb());
        dimensions.setLengthCm(product.getLengthCm());
        dimensions.setLengthIn(product.getLengthIn());
        dimensions.setWidthCm(product.getWidthCm());
        dimensions.setWidthIn(product.getWidthIn());
        dimensions.setHeightCm(product.getHeightCm());
        dimensions.setHeightIn(product.getHeightIn());
        dto.setDimensions(dimensions);
        return dto;
    }

    private CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }

    private String resolvePrimaryImage(Product product) {
        ProductImage primary = product.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .findFirst()
                .orElseGet(() -> product.getImages().stream().findFirst().orElse(null));
        if (primary == null) {
            return null;
        }

        for (String url : List.of(primary.effectiveMediumPath(), primary.getUrlPath(), primary.effectiveThumbPath())) {
            if (url != null && fileStorageService.productImageExists(url)) {
                return url;
            }
        }
        return null;
    }

    private ProductSuggestionDto toSuggestionDto(Product product) {
        ProductSuggestionDto dto = new ProductSuggestionDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategorySlug(product.getCategory().getSlug());
        dto.setPrice(product.getPrice());
        dto.setPrimaryImageUrl(resolvePrimaryImage(product));
        return dto;
    }
}
