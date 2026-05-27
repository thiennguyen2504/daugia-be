package com.example.daugia.backup.storage;

import com.example.daugia.backup.entity.BackupType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BackupStorageProvider {

    Path resolveBackupPath(BackupType type, String fileName);

    void ensureDirectory(Path path) throws IOException;

    OutputStream openOutputStream(Path path) throws IOException;

    InputStream openInputStream(Path path) throws IOException;

    boolean exists(Path path);

    long size(Path path) throws IOException;

    void delete(Path path) throws IOException;
}
