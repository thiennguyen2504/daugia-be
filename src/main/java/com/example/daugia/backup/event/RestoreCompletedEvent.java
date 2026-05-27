package com.example.daugia.backup.event;

import com.example.daugia.common.event.DomainEvent;

public class RestoreCompletedEvent extends DomainEvent {

    private final String restoreId;
    private final String triggeredBy;

    public RestoreCompletedEvent(String restoreId, String triggeredBy) {
        this.restoreId = restoreId;
        this.triggeredBy = triggeredBy;
    }

    public String getRestoreId() {
        return restoreId;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    @Override
    public String getAggregateType() {
        return "RESTORE";
    }

    @Override
    public String getAggregateId() {
        return restoreId;
    }
}
