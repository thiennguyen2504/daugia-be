package com.example.daugia.common.event;

public class CategoryUpdatedEvent extends DomainEvent {

    private final String categoryId;
    private final String categoryName;
    private final String previousName;

    public CategoryUpdatedEvent(String categoryId, String categoryName, String previousName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.previousName = previousName;
    }

    public String getCategoryId() {
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
        return categoryId;
    }
}
