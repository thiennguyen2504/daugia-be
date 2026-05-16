package com.example.daugia.common.event;

public class CategoryCreatedEvent extends DomainEvent {

    private final String categoryId;
    private final String categoryName;

    public CategoryCreatedEvent(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public String getCategoryId() {
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
        return categoryId;
    }
}
