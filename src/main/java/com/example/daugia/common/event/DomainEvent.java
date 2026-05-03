package com.example.daugia.common.event;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();

    public abstract String getAggregateType();

    public abstract String getAggregateId();

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
