package com.lampify.config;

import com.lampify.repository.ProductRepository;
import com.lampify.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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
    public void run(ApplicationArguments args) {
        productRepository.findAll().forEach(product -> {
            try {
                fileStorageService.ensureProductPlaceholder(product.getId());
            } catch (Exception ex) {
                log.warn("Could not create placeholder image for product {}: {}", product.getId(), ex.getMessage());
            }
        });
    }
}
