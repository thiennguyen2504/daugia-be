package com.example.daugia.common.event;

public class CategoryDeletedEvent extends DomainEvent {

    private final Long categoryId;
    private final String categoryName;

    public CategoryDeletedEvent(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    @Override
    public String getAggregateType() {
        return "CATEGORY";
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(categoryId);
    }
}
