package com.example.daugia.backup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backup_records", indexes = {
    @Index(name = "idx_backup_status", columnList = "status"),
    @Index(name = "idx_backup_created", columnList = "created_at"),
    @Index(name = "idx_backup_type", columnList = "type")
})
public class BackupRecord {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupStatus status;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BackupStatus.PENDING;
        }
    }
}
