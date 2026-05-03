package com.example.daugia.common.event;

public class CategoryCreatedEvent extends DomainEvent {

    private final Long categoryId;
    private final String categoryName;

    public CategoryCreatedEvent(Long categoryId, String categoryName) {
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
