package com.lampify.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final Set<String> SKIP_RESIZE_EXTENSIONS = Set.of("svg", "gif");
    private static final String DEFAULT_PRODUCT_IMAGE = "catalog/default-product.png";
    private static final String SEED_IMAGE_DIR = "catalog/seed/";
    private static final String PLACEHOLDER_FILE_NAME = "placeholder.png";
    static final int THUMB_SIZE = 150;
    static final int MEDIUM_SIZE = 600;

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
        Path onDisk = uploadRoot().resolve(relative).normalize();
        if (onDisk.startsWith(uploadRoot()) && Files.isRegularFile(onDisk)) {
            return true;
        }
        return classpathSeedExists(fileNameOf(relative));
    }

    /**
     * Disk file first, then the matching file in {@code catalog/seed/} (used on Render
     * when the ephemeral upload directory is empty).
     */
    public Resource loadPublicUpload(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")) {
            return null;
        }
        String relative = relativePath.replace('\\', '/');
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        Path onDisk = uploadRoot().resolve(relative).normalize();
        if (onDisk.startsWith(uploadRoot()) && Files.isRegularFile(onDisk)) {
            return new FileSystemResource(onDisk);
        }

        try (InputStream inputStream = openClasspathSeed(fileNameOf(relative))) {
            if (inputStream == null) {
                return null;
            }
            return new ByteArrayResource(inputStream.readAllBytes());
        }
    }

    public void ensureProductPlaceholder(Long productId) throws IOException {
        ensureSeedImage(productId, PLACEHOLDER_FILE_NAME);
    }

    public void ensureSeedImage(Long productId, String fileName) throws IOException {
        String resolvedName = (fileName == null || fileName.isBlank()) ? PLACEHOLDER_FILE_NAME : fileName.trim();
        Path productDir = uploadRoot().resolve(Paths.get("products", String.valueOf(productId)));
        Path target = productDir.resolve(resolvedName);
        Files.createDirectories(productDir);

        try (InputStream inputStream = openClasspathSeed(resolvedName)) {
            if (inputStream != null) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                writeSeedVariants(productDir, resolvedName, target);
                return;
            }
        }

        if (Files.exists(target)) {
            return;
        }

        target = productPlaceholderPath(productId);
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = new ClassPathResource(DEFAULT_PRODUCT_IMAGE).getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeSeedVariants(Path productDir, String fileName, Path fullPath) {
        String extension = extractExtension(fileName);
        if (SKIP_RESIZE_EXTENSIONS.contains(extension)) {
            return;
        }
        try {
            BufferedImage original = ImageIO.read(fullPath.toFile());
            if (original == null) {
                return;
            }
            Path mediumDir = productDir.resolve("medium");
            Path thumbDir = productDir.resolve("thumb");
            Files.createDirectories(mediumDir);
            Files.createDirectories(thumbDir);
            writeVariant(original, mediumDir.resolve(fileName), MEDIUM_SIZE, extension);
            writeVariant(original, thumbDir.resolve(fileName), THUMB_SIZE, extension);
        } catch (Exception ignored) {
            // Listing falls back to the full image when variants are missing.
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
        Path productDir = uploadRoot().resolve(Paths.get("products", String.valueOf(productId)));
        Files.createDirectories(productDir);

        Path fullPath = productDir.resolve(storedName);
        byte[] bytes = file.getBytes();
        Files.write(fullPath, bytes);

        String urlPath = "/uploads/products/" + productId + "/" + storedName;
        String thumbPath = urlPath;
        String mediumPath = urlPath;

        if (!SKIP_RESIZE_EXTENSIONS.contains(extension)) {
            try {
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
                if (original != null) {
                    Path mediumDir = productDir.resolve("medium");
                    Path thumbDir = productDir.resolve("thumb");
                    Files.createDirectories(mediumDir);
                    Files.createDirectories(thumbDir);
                    writeVariant(original, mediumDir.resolve(storedName), MEDIUM_SIZE, extension);
                    writeVariant(original, thumbDir.resolve(storedName), THUMB_SIZE, extension);
                    mediumPath = "/uploads/products/" + productId + "/medium/" + storedName;
                    thumbPath = "/uploads/products/" + productId + "/thumb/" + storedName;
                }
            } catch (Exception ignored) {
                thumbPath = urlPath;
                mediumPath = urlPath;
            }
        }

        return new StoredFile(storedName, urlPath, thumbPath, mediumPath);
    }

    private void writeVariant(BufferedImage source, Path target, int maxSize, String extension) throws IOException {
        String format = "jpg".equals(extension) || "jpeg".equals(extension) ? "jpg" : extension;
        if (!Set.of("jpg", "png", "webp").contains(format)) {
            format = "png";
        }
        Thumbnails.of(source)
                .size(maxSize, maxSize)
                .keepAspectRatio(true)
                .outputFormat(format)
                .toFile(target.toFile());
    }

    private static String fileNameOf(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static InputStream openClasspathSeed(String fileName) {
        if (fileName == null || !fileName.matches("[A-Za-z0-9._-]+")) {
            return null;
        }
        return FileStorageService.class.getClassLoader().getResourceAsStream(SEED_IMAGE_DIR + fileName);
    }

    private static boolean classpathSeedExists(String fileName) {
        try (InputStream inputStream = openClasspathSeed(fileName)) {
            return inputStream != null;
        } catch (IOException ex) {
            return false;
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    public record StoredFile(String fileName, String urlPath, String thumbPath, String mediumPath) {}
}
