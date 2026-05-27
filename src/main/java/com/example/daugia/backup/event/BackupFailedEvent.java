package com.example.daugia.backup.event;

import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.common.event.DomainEvent;

public class BackupFailedEvent extends DomainEvent {

    private final String backupId;
    private final BackupType backupType;
    private final String errorMessage;

    public BackupFailedEvent(String backupId, BackupType backupType, String errorMessage) {
        this.backupId = backupId;
        this.backupType = backupType;
        this.errorMessage = errorMessage;
    }

    public String getBackupId() {
        return backupId;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getAggregateType() {
        return "BACKUP";
    }

    @Override
    public String getAggregateId() {
        return backupId;
    }
}
