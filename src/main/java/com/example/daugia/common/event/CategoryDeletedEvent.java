package com.example.daugia.common.event;

public class CategoryDeletedEvent extends DomainEvent {

    private final String categoryId;
    private final String categoryName;

    public CategoryDeletedEvent(String categoryId, String categoryName) {
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
