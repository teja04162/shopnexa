package com.shopnexa.service;

import com.shopnexa.dto.CategoryRequest;
import com.shopnexa.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getActiveCategories();

    CategoryResponse getActiveCategory(Long id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
