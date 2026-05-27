package com.example.daugia.backup.service;

import com.example.daugia.backup.dto.BackupStatusResponse;
import com.example.daugia.backup.dto.RestoreResult;
import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface BackupService {

    BackupRecord triggerFullBackup(String triggeredBy);

    RestoreResult restoreFromBackup(String backupId, String adminEmail);


    Page<BackupRecord> listBackups(Pageable pageable, BackupType type, BackupStatus status);

    BackupRecord getBackup(String backupId);

    BackupStatusResponse getStatus();

    BackupRecord softDelete(String backupId, String adminEmail);

    void applyRetentionPolicy();
}
