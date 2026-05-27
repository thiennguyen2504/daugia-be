package com.example.daugia.backup.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("backup")
public record BackupProperties(
        boolean enabled,
        Full full,
        Wal wal,
        Retention retention,
        List<String> adminEmails,
        boolean encrypt
) {
    public BackupProperties {
        full = full == null ? new Full("0 0 2 * * SUN", "/backups/full") : full;
        wal = wal == null ? new Wal("/backups/wal") : wal;
        retention = retention == null ? new Retention(4, 14, 86_400_000L) : retention;
        adminEmails = adminEmails == null ? List.of() : adminEmails;
    }

    public record Full(String cron, String path) {
        public Full {
            cron = (cron == null || cron.isBlank()) ? "0 0 2 * * SUN" : cron;
            path = (path == null || path.isBlank()) ? "/backups/full" : path;
        }
    }

    public record Wal(String path) {
        public Wal {
            path = (path == null || path.isBlank()) ? "/backups/wal" : path;
        }
    }

    public record Retention(int fullWeeks, int walDays, long checkRate) {
        public Retention {
            fullWeeks = fullWeeks <= 0 ? 4 : fullWeeks;
            walDays = walDays <= 0 ? 14 : walDays;
            checkRate = checkRate <= 0 ? 86_400_000L : checkRate;
        }
    }
}
