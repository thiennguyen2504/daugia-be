package com.example.daugia.backup.service;

import com.example.daugia.auth.service.EmailService;
import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.executor.BackupProcessExecutor;
import com.example.daugia.backup.executor.FullBackupExecutor;
import com.example.daugia.backup.executor.ProcessResult;
import com.example.daugia.backup.properties.BackupProperties;
import com.example.daugia.backup.repository.BackupRecordRepository;
import com.example.daugia.backup.storage.BackupStorageProvider;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock BackupRecordRepository backupRecordRepository;
    @Mock BackupStorageProvider storageProvider;
    @Mock BackupProcessExecutor processExecutor;
    @Mock AuditService auditService;
    @Mock EmailService emailService;
    @Mock DomainEventPublisher eventPublisher;

    private BackupService backupService;
    private final AtomicReference<BackupRecord> store = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        BackupProperties properties = new BackupProperties(true,
                new BackupProperties.Full("0 0 2 * * SUN", "/backups/full"),
                new BackupProperties.Retention(4, 86_400_000L),
                java.util.List.of("admin@example.com"),
                false);

        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl("jdbc:postgresql://db:5432/daugia");
        dataSourceProperties.setUsername("db_user");
        dataSourceProperties.setPassword("db_password");

        FullBackupExecutor fullBackupExecutor = new FullBackupExecutor(storageProvider, processExecutor);
        Executor executor = Runnable::run;

        backupService = new BackupServiceImpl(
                backupRecordRepository,
                storageProvider,
                processExecutor,
                fullBackupExecutor,
                properties,
                auditService,
                emailService,
                eventPublisher,
                dataSourceProperties,
                executor
        );

        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(invocation -> {
            BackupRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId("backup-1");
            }
            if (record.getCreatedAt() == null) {
                record.setCreatedAt(LocalDateTime.now());
            }
            store.set(record);
            return record;
        });
        when(backupRecordRepository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(store.get()));

        Path backupPath = Path.of("/backups/full/test.sql.gz");
        when(storageProvider.resolveBackupPath(eq(BackupType.FULL), anyString())).thenReturn(backupPath);
        doNothing().when(storageProvider).ensureDirectory(eq(backupPath));
        when(storageProvider.openOutputStream(eq(backupPath))).thenReturn(new ByteArrayOutputStream());
        lenient().when(storageProvider.size(eq(backupPath))).thenReturn(1024L);
        lenient().when(storageProvider.openInputStream(eq(backupPath)))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

        when(processExecutor.executeAndStreamOutput(any(), any(), any())).thenReturn(new ProcessResult(0, ""));
    }

    @Test
    void triggerFullBackupRunsAsyncAndUpdatesRecord() {
        BackupRecord record = backupService.triggerFullBackup("admin@example.com");

        assertThat(record.getId()).isEqualTo("backup-1");
        assertThat(store.get().getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(store.get().getFileName()).contains("full.sql.gz");
        assertThat(store.get().getFileSizeBytes()).isEqualTo(1024L);
        verify(eventPublisher).publish(any());
    }

    @Test
    void backupFailureNotifiesAdmins() {
        when(processExecutor.executeAndStreamOutput(any(), any(), any()))
                .thenReturn(new ProcessResult(1, "pg_dump failed"));

        backupService.triggerFullBackup("admin@example.com");

        assertThat(store.get().getStatus()).isEqualTo(BackupStatus.FAILED);
        verify(emailService).sendBackupFailureAlert(any(), eq("FULL"), any());
    }
}
