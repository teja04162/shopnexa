package com.shopnexa.service.impl;

import com.shopnexa.dto.CategoryRequest;
import com.shopnexa.dto.CategoryResponse;
import com.shopnexa.entity.Category;
import com.shopnexa.exception.ConflictException;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.repository.CategoryRepository;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getActiveCategory(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category was not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = normalizeSlug(request.slug(), request.name(), 120);
        ensureSlugAvailable(slug, null);
        Category category = new Category(
                request.name().trim(),
                slug,
                cleanDescription(request.description()),
                request.active() == null || request.active()
        );
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category was not found."));
        String slug = normalizeSlug(request.slug(), request.name(), 120);
        ensureSlugAvailable(slug, id);
        category.update(
                request.name().trim(),
                slug,
                cleanDescription(request.description()),
                request.active() == null ? category.isActive() : request.active()
        );
        return toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category was not found."));
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category cannot be deleted while products are assigned to it.");
        }
        categoryRepository.delete(category);
    }

    private void ensureSlugAvailable(String slug, Long currentId) {
        boolean exists = currentId == null
                ? categoryRepository.existsBySlugIgnoreCase(slug)
                : categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
        if (exists) {
            throw new ConflictException("A category with this slug already exists.");
        }
    }

    private String normalizeSlug(String requestedSlug, String name, int maxLength) {
        String source = requestedSlug == null || requestedSlug.isBlank() ? name : requestedSlug;
        String slug = Normalizer.normalize(source.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) {
            throw new BadRequestException("Slug must contain at least one letter or number.");
        }
        return slug.substring(0, Math.min(slug.length(), maxLength)).replaceAll("-+$", "");
    }

    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive()
        );
    }
}
