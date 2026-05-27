package com.example.daugia.backup.storage;

import com.example.daugia.backup.entity.BackupType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "backup.storage", havingValue = "cloud")
public class CloudBackupStorageProvider implements BackupStorageProvider {

    @Override
    public Path resolveBackupPath(BackupType type, String fileName) {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }

    @Override
    public void ensureDirectory(Path path) {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }

    @Override
    public OutputStream openOutputStream(Path path) throws IOException {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }

    @Override
    public InputStream openInputStream(Path path) throws IOException {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }

    @Override
    public boolean exists(Path path) {
        return false;
    }

    @Override
    public long size(Path path) throws IOException {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("Cloud backup storage is not configured");
    }
}
