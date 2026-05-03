package com.example.daugia.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DomainEventListener {

    @EventListener
    public void onCategoryCreated(CategoryCreatedEvent event) {
        log.info("[EVENT] CategoryCreated — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
        // TODO: trigger cache invalidation, search index update
    }

    @EventListener
    public void onCategoryUpdated(CategoryUpdatedEvent event) {
        log.info("[EVENT] CategoryUpdated — id={}, newName={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @EventListener
    public void onCategoryDeleted(CategoryDeletedEvent event) {
        log.info("[EVENT] CategoryDeleted — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }
}
