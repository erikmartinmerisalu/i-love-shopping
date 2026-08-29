package com.lampify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @Test
    void rasterUploadWritesThumbMediumAndFull() throws Exception {
        BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 400, 300);
        graphics.dispose();

        Path png = tempDir.resolve("source.png");
        ImageIO.write(source, "png", png.toFile());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lamp.png",
                "image/png",
                Files.readAllBytes(png));

        FileStorageService.StoredFile stored = fileStorageService.storeProductImage(42L, file);

        assertTrue(fileStorageService.productImageExists(stored.urlPath()));
        assertTrue(fileStorageService.productImageExists(stored.thumbPath()));
        assertTrue(fileStorageService.productImageExists(stored.mediumPath()));
        assertNotEquals(stored.urlPath(), stored.thumbPath());
        assertTrue(stored.thumbPath().contains("/thumb/"));
        assertTrue(stored.mediumPath().contains("/medium/"));
    }

    @Test
    void svgUploadReusesFullPathForVariants() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "icon.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes());

        FileStorageService.StoredFile stored = fileStorageService.storeProductImage(7L, file);

        assertEquals(stored.urlPath(), stored.thumbPath());
        assertEquals(stored.urlPath(), stored.mediumPath());
        assertTrue(fileStorageService.productImageExists(stored.urlPath()));
    }

    @Test
    void seedJpegWritesThumbAndMediumVariants() throws Exception {
        fileStorageService.ensureSeedImage(3L, "smart-bulb.jpg");

        assertTrue(fileStorageService.productImageExists("/uploads/products/3/smart-bulb.jpg"));
        assertTrue(fileStorageService.productImageExists("/uploads/products/3/thumb/smart-bulb.jpg"));
        assertTrue(fileStorageService.productImageExists("/uploads/products/3/medium/smart-bulb.jpg"));
    }
}
