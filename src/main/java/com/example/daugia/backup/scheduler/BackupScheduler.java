package com.example.daugia.backup.scheduler;

import com.example.daugia.backup.service.BackupService;
import com.example.daugia.common.logging.LogContext;
import com.example.daugia.common.logging.LogField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "backup.enabled", havingValue = "true")
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "${backup.full.cron:0 0 2 * * SUN}")
    @SchedulerLock(name = "fullBackupScheduler", lockAtMostFor = "PT4H", lockAtLeastFor = "PT1M")
    public void scheduleFullBackup() {
        try (var ctx = LogContext.of(LogField.TRACE_ID, UUID.randomUUID().toString())
                .and(LogField.ACTOR, "SCHEDULER")
                .and(LogField.OPERATION, "full-backup")
                .build()) {
            log.info("Scheduled full backup triggered");
            backupService.triggerFullBackup("SCHEDULER");
        }
    }

    @Scheduled(fixedRateString = "${backup.retention.check-rate:86400000}")
    @SchedulerLock(name = "backupRetentionScheduler", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void scheduleRetention() {
        try (var ctx = LogContext.of(LogField.TRACE_ID, UUID.randomUUID().toString())
                .and(LogField.ACTOR, "SCHEDULER")
                .and(LogField.OPERATION, "backup-retention")
                .build()) {
            log.info("Running backup retention policy");
            backupService.applyRetentionPolicy();
        }
    }
}
