package com.example.daugia.backup.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("backup")
public record BackupProperties(
        boolean enabled,
        Full full,
        Retention retention,
        List<String> adminEmails,
        boolean encrypt
) {
    public BackupProperties {
        full = full == null ? new Full("0 0 2 * * SUN", "/backups/full") : full;
        retention = retention == null ? new Retention(4, 86_400_000L) : retention;
        adminEmails = adminEmails == null ? List.of() : adminEmails;
    }

    public record Full(String cron, String path) {
        public Full {
            cron = (cron == null || cron.isBlank()) ? "0 0 2 * * SUN" : cron;
            path = (path == null || path.isBlank()) ? "/backups/full" : path;
        }
    }

    public record Retention(int fullWeeks, long checkRate) {
        public Retention {
            fullWeeks = fullWeeks <= 0 ? 4 : fullWeeks;
            checkRate = checkRate <= 0 ? 86_400_000L : checkRate;
        }
    }
}
