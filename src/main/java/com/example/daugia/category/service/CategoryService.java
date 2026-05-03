package com.example.daugia.category.service;

import com.example.daugia.category.dto.CategoryRequest;
import com.example.daugia.category.dto.CategoryResponse;
import com.example.daugia.common.dto.PageResponse;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request, String createdByEmail);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
    CategoryResponse getById(Long id);
    PageResponse<CategoryResponse> getAll(int page, int size, String search);
}
