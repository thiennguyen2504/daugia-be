package com.example.daugia.backup.dto;

import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponse {
    private String id;
    private BackupType type;
    private BackupStatus status;
    private String fileName;
    private String filePath;
    private Long fileSizeBytes;
    private String fileSizeFormatted;
    private Long durationMs;
    private String triggeredBy;
    private String errorMessage;
    private String checksumSha256;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
