package com.lampify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final String DEFAULT_PRODUCT_IMAGE = "catalog/default-product.png";
    private static final String PLACEHOLDER_FILE_NAME = "placeholder.png";

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path productPlaceholderPath(Long productId) {
        return uploadRoot().resolve(Paths.get("products", String.valueOf(productId), PLACEHOLDER_FILE_NAME));
    }

    public boolean productImageExists(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return false;
        }

        String relative = urlPath.startsWith("/uploads/") ? urlPath.substring("/uploads/".length()) : urlPath;
        return Files.isRegularFile(uploadRoot().resolve(relative));
    }

    public void ensureProductPlaceholder(Long productId) throws IOException {
        Path target = productPlaceholderPath(productId);
        if (Files.exists(target)) {
            return;
        }

        Files.createDirectories(target.getParent());
        ClassPathResource resource = new ClassPathResource(DEFAULT_PRODUCT_IMAGE);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public StoredFile storeProductImage(Long productId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image type");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path productDir = Paths.get(uploadDir, "products", String.valueOf(productId));
        Files.createDirectories(productDir);

        Path target = productDir.resolve(storedName);
        Files.copy(file.getInputStream(), target);

        String urlPath = "/uploads/products/" + productId + "/" + storedName;
        return new StoredFile(storedName, urlPath);
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    public record StoredFile(String fileName, String urlPath) {}
}
