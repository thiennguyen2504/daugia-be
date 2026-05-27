package com.example.daugia.backup.event;

import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.common.event.DomainEvent;

public class BackupCompletedEvent extends DomainEvent {

    private final String backupId;
    private final BackupType backupType;
    private final String fileName;

    public BackupCompletedEvent(String backupId, BackupType backupType, String fileName) {
        this.backupId = backupId;
        this.backupType = backupType;
        this.fileName = fileName;
    }

    public String getBackupId() {
        return backupId;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public String getFileName() {
        return fileName;
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
