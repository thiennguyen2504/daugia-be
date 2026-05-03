package com.example.daugia.common.event;

public class CategoryUpdatedEvent extends DomainEvent {

    private final Long categoryId;
    private final String categoryName;
    private final String previousName;

    public CategoryUpdatedEvent(Long categoryId, String categoryName, String previousName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.previousName = previousName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getPreviousName() {
        return previousName;
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
