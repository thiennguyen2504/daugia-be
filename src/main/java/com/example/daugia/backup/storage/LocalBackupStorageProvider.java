package com.example.daugia.backup.storage;

import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.properties.BackupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Primary
@ConditionalOnProperty(name = "backup.storage", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalBackupStorageProvider implements BackupStorageProvider {

    private final BackupProperties backupProperties;

    @Override
    public Path resolveBackupPath(BackupType type, String fileName) {
        String basePath = type == BackupType.WAL
                ? backupProperties.wal().path()
                : backupProperties.full().path();
        return Paths.get(basePath).resolve(fileName);
    }

    @Override
    public void ensureDirectory(Path path) throws IOException {
        Path dir = path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
    }

    @Override
    public OutputStream openOutputStream(Path path) throws IOException {
        return Files.newOutputStream(path);
    }

    @Override
    public InputStream openInputStream(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    @Override
    public long size(Path path) throws IOException {
        return Files.size(path);
    }

    @Override
    public void delete(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
