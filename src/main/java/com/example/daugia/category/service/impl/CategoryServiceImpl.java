package com.example.daugia.category.service.impl;

import com.example.daugia.category.dto.CategoryRequest;
import com.example.daugia.category.dto.CategoryResponse;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.mapper.CategoryMapper;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.category.service.CategoryService;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.common.event.CategoryCreatedEvent;
import com.example.daugia.common.event.CategoryDeletedEvent;
import com.example.daugia.common.event.CategoryUpdatedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.exception.DuplicateResourceException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public CategoryResponse create(CategoryRequest request, String createdByEmail) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + request.getName());
        }

        Category category = categoryMapper.toEntity(request);
        category.setCreatedBy(createdByEmail);
        Category saved = categoryRepository.save(category);

        eventPublisher.publish(new CategoryCreatedEvent(saved.getId(), saved.getName()));

        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Category name already exists: " + request.getName());
        }

        String previousName = category.getName();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);

        eventPublisher.publish(new CategoryUpdatedEvent(saved.getId(), saved.getName(), previousName));

        return categoryMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        category.setDeleted(true);
        categoryRepository.save(category);

        eventPublisher.publish(new CategoryDeletedEvent(category.getId(), category.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAll(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Category> result = (search != null && !search.isBlank())
                ? categoryRepository.findAllByDeletedFalseAndNameContainingIgnoreCase(search, pageable)
                : categoryRepository.findAllByDeletedFalse(pageable);
        return PageResponse.from(result.map(categoryMapper::toResponse));
    }
}
