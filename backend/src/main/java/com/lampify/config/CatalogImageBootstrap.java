package com.lampify.config;

import com.lampify.repository.ProductRepository;
import com.lampify.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogImageBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogImageBootstrap.class);

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public CatalogImageBootstrap(ProductRepository productRepository, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        productRepository.findAllWithImages().forEach(product -> {
            if (product.getImages().isEmpty()) {
                copySeed(product.getId(), "placeholder.png");
                return;
            }
            product.getImages().forEach(image -> copySeed(product.getId(), image.getFileName()));
        });
    }

    private void copySeed(Long productId, String fileName) {
        try {
            fileStorageService.ensureSeedImage(productId, fileName);
        } catch (Exception ex) {
            log.warn("Could not create seed image {} for product {}: {}", fileName, productId, ex.getMessage());
        }
    }
}
