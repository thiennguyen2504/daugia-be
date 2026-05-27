package com.example.daugia.backup.executor;

import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.exception.BackupException;
import com.example.daugia.backup.storage.BackupStorageProvider;
import com.example.daugia.backup.util.ChecksumUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractBackupExecutor {

    protected final BackupStorageProvider storageProvider;
    protected final BackupProcessExecutor processExecutor;

    protected AbstractBackupExecutor(BackupStorageProvider storageProvider, BackupProcessExecutor processExecutor) {
        this.storageProvider = storageProvider;
        this.processExecutor = processExecutor;
    }

    public BackupExecutionResult execute(String fileName, List<String> command, Map<String, String> environment) {
        preExecute(fileName);
        try {
            BackupExecutionResult result = performBackup(fileName, command, environment);
            postExecuteSuccess(result);
            return result;
        } catch (Exception ex) {
            postExecuteFailure(ex);
            if (ex instanceof BackupException backupException) {
                throw backupException;
            }
            throw BackupException.serverError(ex.getMessage());
        }
    }

    protected void preExecute(String fileName) {
        // Hook for subclasses.
    }

    protected void postExecuteSuccess(BackupExecutionResult result) {
        // Hook for subclasses.
    }

    protected void postExecuteFailure(Exception ex) {
        // Hook for subclasses.
    }

    protected BackupExecutionResult performBackup(String fileName, List<String> command, Map<String, String> environment) {
        Path outputPath = storageProvider.resolveBackupPath(getBackupType(), fileName);
        try {
            storageProvider.ensureDirectory(outputPath);
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to create backup directory: " + ex.getMessage());
        }

        try (OutputStream fileOut = storageProvider.openOutputStream(outputPath);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut)) {
            ProcessResult result = processExecutor.executeAndStreamOutput(command, environment, gzipOut);
            if (!result.isSuccess()) {
                throw BackupException.serverError("Backup command failed: " + result.errorOutput());
            }
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to write backup file: " + ex.getMessage());
        }

        try {
            long size = storageProvider.size(outputPath);
            try (var inputStream = storageProvider.openInputStream(outputPath)) {
                String checksum = ChecksumUtils.sha256(inputStream);
                return new BackupExecutionResult(outputPath.toString(), fileName, size, checksum);
            }
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to finalize backup file: " + ex.getMessage());
        }
    }

    protected abstract BackupType getBackupType();
}
