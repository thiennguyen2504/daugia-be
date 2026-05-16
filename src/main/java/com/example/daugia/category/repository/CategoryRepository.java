package com.example.daugia.category.repository;

import com.example.daugia.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
    Optional<Category> findByIdAndDeletedFalse(String id);
    Page<Category> findAllByDeletedFalse(Pageable pageable);
    Page<Category> findAllByDeletedFalseAndNameContainingIgnoreCase(String name, Pageable pageable);
}
