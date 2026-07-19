package com.lampify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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
