package com.lampify.service;

import com.lampify.dto.AdminCategoryRequest;
import com.lampify.dto.CategoryDto;
import com.lampify.entity.Category;
import com.lampify.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminCategoryService(
            CategoryRepository categoryRepository,
            AdminAuthorizationService adminAuthorizationService) {
        this.categoryRepository = categoryRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        adminAuthorizationService.requireAdminWithTwoFactor();
        return categoryRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CategoryDto createCategory(AdminCategoryRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        String slug = request.getSlug().trim().toLowerCase();
        if (categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(trimToNull(request.getDescription()));
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto updateCategory(Long id, AdminCategoryRequest request) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        String slug = request.getSlug().trim().toLowerCase();
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(trimToNull(request.getDescription()));
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        adminAuthorizationService.requireAdminWithTwoFactor();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryRepository.delete(category);
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
