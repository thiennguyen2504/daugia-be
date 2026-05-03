package com.example.daugia.common.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
