package com.example.daugia.backup.repository;

import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, String>, JpaSpecificationExecutor<BackupRecord> {

    Page<BackupRecord> findAllByType(BackupType type, Pageable pageable);

    Page<BackupRecord> findAllByStatus(BackupStatus status, Pageable pageable);

    Page<BackupRecord> findAllByTypeAndStatus(BackupType type, BackupStatus status, Pageable pageable);

    Optional<BackupRecord> findTopByTypeAndStatusOrderByCompletedAtDesc(BackupType type, BackupStatus status);

    Optional<BackupRecord> findTopByTypeAndStatusAndCompletedAtBeforeOrderByCompletedAtDesc(
            BackupType type,
            BackupStatus status,
            LocalDateTime completedAt);

    List<BackupRecord> findAllByTypeAndCreatedAtBeforeAndStatusNot(
            BackupType type,
            LocalDateTime cutoff,
            BackupStatus status);

    List<BackupRecord> findAllByTypeAndStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            BackupType type,
            BackupStatus status,
            LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(b.fileSizeBytes), 0) FROM BackupRecord b WHERE b.status <> 'DELETED'")
    long sumActiveFileSizeBytes();

    @Query("SELECT COUNT(b) FROM BackupRecord b WHERE b.status <> 'DELETED'")
    long countActive();
}
