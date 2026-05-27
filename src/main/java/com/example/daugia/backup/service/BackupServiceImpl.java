package com.example.daugia.backup.service;

import com.example.daugia.backup.command.FullRestoreCommand;
import com.example.daugia.backup.command.RestoreCommand;
import com.example.daugia.backup.dto.BackupResponse;
import com.example.daugia.backup.dto.BackupStatusResponse;
import com.example.daugia.backup.dto.RestoreResponse;
import com.example.daugia.backup.dto.RestoreResult;
import com.example.daugia.backup.dto.RestoreStatus;
import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.event.BackupCompletedEvent;
import com.example.daugia.backup.event.BackupFailedEvent;
import com.example.daugia.backup.event.RestoreCompletedEvent;
import com.example.daugia.backup.exception.BackupException;
import com.example.daugia.backup.executor.BackupExecutionResult;
import com.example.daugia.backup.executor.BackupProcessExecutor;
import com.example.daugia.backup.executor.FullBackupExecutor;
import com.example.daugia.backup.properties.BackupProperties;
import com.example.daugia.backup.repository.BackupRecordRepository;
import com.example.daugia.backup.storage.BackupStorageProvider;
import com.example.daugia.backup.util.SizeFormatter;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.logging.LogContext;
import com.example.daugia.common.logging.LogField;
import com.example.daugia.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "backup.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class BackupServiceImpl implements BackupService {

    private static final String DB_CONTAINER = "daugia-db";
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final BackupRecordRepository backupRecordRepository;
    private final BackupStorageProvider storageProvider;
    private final BackupProcessExecutor processExecutor;
    private final FullBackupExecutor fullBackupExecutor;
    private final BackupProperties backupProperties;
    private final AuditService auditService;
    private final EmailService emailService;
    private final DomainEventPublisher eventPublisher;
    private final DataSourceProperties dataSourceProperties;
    @Qualifier("domainEventExecutor")
    private final Executor domainEventExecutor;

    private final AtomicReference<RestoreResponse> currentRestore = new AtomicReference<>();

    @Override
    public BackupRecord triggerFullBackup(String triggeredBy) {
        BackupRecord record = createInitialRecord(BackupType.FULL, triggeredBy);
        logAudit(triggeredBy, AuditAction.BACKUP_TRIGGERED, record, AuditOutcome.SUCCESS, "Full backup triggered");
        schedule(() -> executeFullBackup(record.getId(), triggeredBy));
        return record;
    }



    @Override
    public RestoreResult restoreFromBackup(String backupId, String adminEmail) {
        BackupRecord record = getBackup(backupId);
        if (record.getStatus() != BackupStatus.SUCCESS) {
            throw BackupException.badRequest("Backup is not in SUCCESS state");
        }
        if (record.getFilePath() == null || record.getFilePath().isBlank()) {
            throw BackupException.badRequest("Backup file path is missing");
        }
        String restoreId = UUID.randomUUID().toString();

        RestoreResponse response = RestoreResponse.builder()
                .restoreId(restoreId)
                .status(RestoreStatus.PENDING)
                .message("Restore scheduled")
                .estimatedDurationMs(null)
                .build();
        currentRestore.set(response);

        logRestoreAudit(adminEmail, AuditAction.RESTORE_TRIGGERED, restoreId, record.getId(), AuditOutcome.SUCCESS, "Restore triggered");
        schedule(() -> executeRestore(restoreId, record, adminEmail));
        return toResult(response);
    }



    @Override
    public Page<BackupRecord> listBackups(Pageable pageable, BackupType type, BackupStatus status) {
        if (type != null && status != null) {
            return backupRecordRepository.findAllByTypeAndStatus(type, status, pageable);
        }
        if (type != null) {
            return backupRecordRepository.findAllByType(type, pageable);
        }
        if (status != null) {
            return backupRecordRepository.findAllByStatus(status, pageable);
        }
        return backupRecordRepository.findAll(pageable);
    }

    @Override
    public BackupRecord getBackup(String backupId) {
        return backupRecordRepository.findById(backupId)
                .orElseThrow(() -> BackupException.notFound("Backup not found"));
    }

    @Override
    public BackupStatusResponse getStatus() {
        Optional<BackupRecord> lastFullBackup = backupRecordRepository
                .findTopByTypeAndStatusOrderByCompletedAtDesc(BackupType.FULL, BackupStatus.SUCCESS);

        CronExpression cronExpression = CronExpression.parse(backupProperties.full().cron());
        ZonedDateTime nextRun = cronExpression.next(ZonedDateTime.now());

        String retentionPolicy = "Full: " + backupProperties.retention().fullWeeks() + " weeks";

        return BackupStatusResponse.builder()
                .lastFullBackup(lastFullBackup.map(this::toResponse).orElse(null))
                .nextScheduledRun(nextRun != null ? nextRun.toLocalDateTime() : null)
                .totalBackups(backupRecordRepository.countActive())
                .totalSizeBytes(backupRecordRepository.sumActiveFileSizeBytes())
                .retentionPolicy(retentionPolicy)
                .currentRestore(currentRestore.get())
                .build();
    }

    @Override
    public BackupRecord softDelete(String backupId, String adminEmail) {
        BackupRecord record = getBackup(backupId);
        record.setStatus(BackupStatus.DELETED);
        record.setErrorMessage("Soft deleted by admin");
        backupRecordRepository.save(record);
        logAudit(adminEmail, AuditAction.BACKUP_COMPLETED, record, AuditOutcome.SUCCESS, "Backup soft deleted");
        return record;
    }

    @Override
    public void applyRetentionPolicy() {
        try (var ctx = LogContext.of(LogField.TRACE_ID, UUID.randomUUID().toString())
                .and(LogField.ACTOR, "SYSTEM")
                .and(LogField.OPERATION, "backup-retention")
                .build()) {
            LocalDateTime fullCutoff = LocalDateTime.now().minusWeeks(backupProperties.retention().fullWeeks());
            expireBackups(BackupType.FULL, fullCutoff);
        }
    }

    private BackupRecord createInitialRecord(BackupType type, String triggeredBy) {
        BackupRecord record = BackupRecord.builder()
                .type(type)
                .status(BackupStatus.PENDING)
                .triggeredBy(triggeredBy)
                .build();
        return backupRecordRepository.save(record);
    }

    private void executeFullBackup(String backupId, String triggeredBy) {
        executeBackup(backupId, triggeredBy, BackupType.FULL);
    }

    private void executeBackup(String backupId, String triggeredBy, BackupType type) {
        String traceId = UUID.randomUUID().toString();
        String operation = "full-backup";

        try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
                .and(LogField.ACTOR, triggeredBy)
                .and(LogField.OPERATION, operation)
                .build()) {

            BackupRecord record = getBackup(backupId);
            record.setStatus(BackupStatus.RUNNING);
            backupRecordRepository.save(record);

            LocalDateTime started = LocalDateTime.now();
            String fileName = buildFileName(type, started);

            BackupExecutionResult result = fullBackupExecutor.execute(fileName, buildPgDumpCommand(), null);

            record.setStatus(BackupStatus.SUCCESS);
            record.setFileName(result.fileName());
            record.setFilePath(result.filePath());
            record.setFileSizeBytes(result.fileSizeBytes());
            record.setChecksumSha256(result.checksumSha256());
            record.setCompletedAt(LocalDateTime.now());
            record.setDurationMs(Duration.between(started, record.getCompletedAt()).toMillis());
            backupRecordRepository.save(record);

            log.info("Backup completed: id={} type={} file={} size={}", record.getId(), type,
                    record.getFileName(), record.getFileSizeBytes());

            logAudit(triggeredBy, AuditAction.BACKUP_COMPLETED, record, AuditOutcome.SUCCESS, "Backup completed");
            eventPublisher.publish(new BackupCompletedEvent(record.getId(), type, record.getFileName()));
        } catch (Exception ex) {
            markBackupFailed(traceId, backupId, triggeredBy, type, ex);
        }
    }

    private void markBackupFailed(String traceId, String backupId, String triggeredBy, BackupType type, Exception ex) {
        try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
                .and(LogField.ACTOR, triggeredBy)
                .and(LogField.OPERATION, "full-backup")
                .build()) {
            BackupRecord record = getBackup(backupId);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(ex.getMessage());
            record.setCompletedAt(LocalDateTime.now());
            backupRecordRepository.save(record);

            log.error("Backup failed: id={} type={} error={}", record.getId(), type, ex.getMessage());
            logAudit(triggeredBy, AuditAction.BACKUP_FAILED, record, AuditOutcome.FAILURE, ex.getMessage());
            eventPublisher.publish(new BackupFailedEvent(record.getId(), type, ex.getMessage()));
            notifyBackupFailure(type, ex.getMessage());
        }
    }

    private void executeRestore(String restoreId, BackupRecord record, String adminEmail) {
        String traceId = UUID.randomUUID().toString();

        try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
                .and(LogField.ACTOR, adminEmail)
                .and(LogField.OPERATION, "restore")
                .build()) {

                updateRestoreStatus(restoreId, RestoreStatus.RUNNING, "Validating checksum");
                RestoreCommand command = new FullRestoreCommand(
                    restoreId,
                    record,
                    Path.of(record.getFilePath()),
                    storageProvider,
                    processExecutor,
                    buildRestoreCommand());

                updateRestoreStatus(restoreId, RestoreStatus.RUNNING, "Stopping writes");
                updateRestoreStatus(restoreId, RestoreStatus.RUNNING, "Restoring database");
            command.execute();
                updateRestoreStatus(restoreId, RestoreStatus.RUNNING, "Restarting services");
                updateRestoreStatus(restoreId, RestoreStatus.SUCCESS, "Restore completed");
            logRestoreAudit(adminEmail, AuditAction.RESTORE_COMPLETED, restoreId, record.getId(), AuditOutcome.SUCCESS, "Restore completed");
            eventPublisher.publish(new RestoreCompletedEvent(restoreId, adminEmail));
        } catch (Exception ex) {
            handleRestoreFailure(traceId, restoreId, record, adminEmail, ex, "restore");
        }
    }



    private void expireBackups(BackupType type, LocalDateTime cutoff) {
        List<BackupRecord> expired = backupRecordRepository
                .findAllByTypeAndCreatedAtBeforeAndStatusNot(type, cutoff, BackupStatus.DELETED);

        for (BackupRecord record : expired) {
            try {
                if (record.getFilePath() != null) {
                    storageProvider.delete(Path.of(record.getFilePath()));
                }
                record.setStatus(BackupStatus.DELETED);
                record.setErrorMessage("Deleted by retention policy");
                backupRecordRepository.save(record);
                logAudit("SCHEDULER", AuditAction.BACKUP_COMPLETED, record, AuditOutcome.SUCCESS, "Retention delete");
            } catch (Exception ex) {
                log.error("Failed to delete backup file: id={} error={}", record.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void notifyBackupFailure(BackupType type, String errorMessage) {
        List<String> recipients = backupProperties.adminEmails().stream()
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            return;
        }
        try {
            emailService.sendBackupFailureAlert(recipients, type.name(), errorMessage);
        } catch (Exception ex) {
            log.error("Failed to send backup failure alert: {}", ex.getMessage(), ex);
        }
    }

    private BackupResponse toResponse(BackupRecord record) {
        return BackupResponse.builder()
                .id(record.getId())
                .type(record.getType())
                .status(record.getStatus())
                .fileName(record.getFileName())
                .filePath(record.getFilePath())
                .fileSizeBytes(record.getFileSizeBytes())
                .fileSizeFormatted(SizeFormatter.formatBytes(record.getFileSizeBytes()))
                .durationMs(record.getDurationMs())
                .triggeredBy(record.getTriggeredBy())
                .errorMessage(record.getErrorMessage())
                .checksumSha256(record.getChecksumSha256())
                .createdAt(record.getCreatedAt())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private RestoreResult toResult(RestoreResponse response) {
        return new RestoreResult(response.getRestoreId(), response.getStatus(), response.getMessage(), response.getEstimatedDurationMs());
    }

    private void updateRestoreStatus(String restoreId, RestoreStatus status, String message) {
        RestoreResponse response = RestoreResponse.builder()
                .restoreId(restoreId)
                .status(status)
                .message(message)
                .estimatedDurationMs(null)
                .build();
        currentRestore.set(response);
    }

    private void logAudit(String actor, AuditAction action, BackupRecord record, AuditOutcome outcome, String message) {
        auditService.log(actor, action, "BACKUP", record.getId(), outcome,
                AuditJsonUtils.toJson("type", record.getType(), "status", record.getStatus(), "message", message));
    }

    private void logRestoreAudit(String actor, AuditAction action, String restoreId, String backupId, AuditOutcome outcome, String message) {
        auditService.log(actor, action, "RESTORE", restoreId, outcome,
                AuditJsonUtils.toJson("backupId", backupId, "message", message));
    }

    private void handleRestoreFailure(String traceId, String restoreId, BackupRecord record, String adminEmail, Exception ex, String operation) {
        try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
                .and(LogField.ACTOR, adminEmail)
                .and(LogField.OPERATION, operation)
                .build()) {
            updateRestoreStatus(restoreId, RestoreStatus.FAILED, ex.getMessage());
            logRestoreAudit(adminEmail, AuditAction.RESTORE_FAILED, restoreId, record.getId(), AuditOutcome.FAILURE, ex.getMessage());
            log.error("Restore failed: id={} error={}", restoreId, ex.getMessage(), ex);
        }
    }

    private void schedule(Runnable task) {
        domainEventExecutor.execute(task);
    }

    private String buildFileName(BackupType type, LocalDateTime timestamp) {
        String suffix = "full.sql.gz";
        return FILE_STAMP.format(timestamp) + "_" + suffix;
    }

    private List<String> buildPgDumpCommand() {
        String username = sanitizeIdentifier(dataSourceProperties.getUsername(), "DB_USER");
        String dbName = sanitizeIdentifier(extractDatabaseName(dataSourceProperties.getUrl()), "DB_NAME");
        String password = dataSourceProperties.getPassword();

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("exec");
        command.add("-i");
        if (password != null && !password.isBlank()) {
            command.add("-e");
            command.add("PGPASSWORD=" + password);
        }
        command.add(DB_CONTAINER);
        command.add("pg_dump");
        command.add("-c");
        command.add("--if-exists");
        command.add("-O");
        command.add("-x");
        command.add("-T");
        command.add("backup_records");
        command.add("-T");
        command.add("audit_logs");
        command.add("-U");
        command.add(username);
        command.add("-d");
        command.add(dbName);
        return command;
    }



    private List<String> buildRestoreCommand() {
        String username = sanitizeIdentifier(dataSourceProperties.getUsername(), "DB_USER");
        String dbName = sanitizeIdentifier(extractDatabaseName(dataSourceProperties.getUrl()), "DB_NAME");
        String password = dataSourceProperties.getPassword();

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("exec");
        command.add("-i");
        if (password != null && !password.isBlank()) {
            command.add("-e");
            command.add("PGPASSWORD=" + password);
        }
        command.add(DB_CONTAINER);
        command.add("psql");
        command.add("-v");
        command.add("ON_ERROR_STOP=1");
        command.add("-U");
        command.add(username);
        command.add("-d");
        command.add(dbName);
        return command;
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) {
            throw BackupException.badRequest("Database URL is missing");
        }
        int slashIndex = jdbcUrl.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == jdbcUrl.length() - 1) {
            throw BackupException.badRequest("Unable to parse database name");
        }
        String dbName = jdbcUrl.substring(slashIndex + 1);
        int queryIndex = dbName.indexOf('?');
        if (queryIndex > 0) {
            dbName = dbName.substring(0, queryIndex);
        }
        return dbName;
    }

    private String sanitizeIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw BackupException.badRequest(fieldName + " is missing");
        }
        if (!value.matches("[A-Za-z0-9_.-]+")) {
            throw BackupException.badRequest(fieldName + " contains invalid characters");
        }
        return value;
    }
}
