package com.example.daugia.backup.dto;

public record RestoreResult(
        String restoreId,
        RestoreStatus status,
        String message,
        Long estimatedDurationMs
) {
}
