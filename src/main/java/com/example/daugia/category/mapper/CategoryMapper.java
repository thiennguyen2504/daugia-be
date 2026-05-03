package com.example.daugia.category.mapper;

import com.example.daugia.category.dto.CategoryRequest;
import com.example.daugia.category.dto.CategoryResponse;
import com.example.daugia.category.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
    Category toEntity(CategoryRequest request);
}
