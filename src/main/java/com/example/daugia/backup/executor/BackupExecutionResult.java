package com.example.daugia.backup.executor;

public record BackupExecutionResult(
        String filePath,
        String fileName,
        long fileSizeBytes,
        String checksumSha256
) {
}
