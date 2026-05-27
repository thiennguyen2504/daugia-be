package com.example.daugia.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupStatusResponse {
    private BackupResponse lastFullBackup;
    private LocalDateTime nextScheduledRun;
    private long totalBackups;
    private long totalSizeBytes;
    private String retentionPolicy;
    private RestoreResponse currentRestore;
}
