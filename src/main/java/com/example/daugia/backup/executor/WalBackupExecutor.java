package com.example.daugia.backup.executor;

import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.storage.BackupStorageProvider;
import org.springframework.stereotype.Component;

@Component
public class WalBackupExecutor extends AbstractBackupExecutor {

    public WalBackupExecutor(BackupStorageProvider storageProvider, BackupProcessExecutor processExecutor) {
        super(storageProvider, processExecutor);
    }

    @Override
    protected BackupType getBackupType() {
        return BackupType.WAL;
    }
}
