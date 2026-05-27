package com.example.daugia.backup.command;

import com.example.daugia.backup.dto.RestoreResult;
import com.example.daugia.backup.dto.RestoreStatus;
import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.exception.BackupException;
import com.example.daugia.backup.executor.BackupProcessExecutor;
import com.example.daugia.backup.executor.ProcessResult;
import com.example.daugia.backup.storage.BackupStorageProvider;
import com.example.daugia.backup.util.ChecksumUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FullRestoreCommand implements RestoreCommand {

    private final String restoreId;
    private final BackupRecord backupRecord;
    private final Path backupFile;
    private final BackupStorageProvider storageProvider;
    private final BackupProcessExecutor processExecutor;
    private final List<String> restoreCommand;

    public FullRestoreCommand(String restoreId,
                              BackupRecord backupRecord,
                              Path backupFile,
                              BackupStorageProvider storageProvider,
                              BackupProcessExecutor processExecutor,
                              List<String> restoreCommand) {
        this.restoreId = restoreId;
        this.backupRecord = backupRecord;
        this.backupFile = backupFile;
        this.storageProvider = storageProvider;
        this.processExecutor = processExecutor;
        this.restoreCommand = restoreCommand;
    }

    @Override
    public RestoreResult execute() {
        if (!storageProvider.exists(backupFile)) {
            throw BackupException.notFound("Backup file not found: " + backupFile);
        }

        validateChecksum();

        try (InputStream fileIn = storageProvider.openInputStream(backupFile);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn)) {
            ProcessResult result = processExecutor.executeWithInput(restoreCommand, null, gzipIn);
            if (!result.isSuccess()) {
                throw BackupException.serverError("Restore command failed: " + result.errorOutput());
            }
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to restore backup: " + ex.getMessage());
        }

        return new RestoreResult(restoreId, RestoreStatus.SUCCESS, "Restore completed", null);
    }

    @Override
    public void rollback(Exception ex) {
        // No rollback hook for now; restoring is destructive.
    }

    private void validateChecksum() {
        if (backupRecord.getChecksumSha256() == null) {
            return;
        }
        try (InputStream inputStream = storageProvider.openInputStream(backupFile)) {
            String computed = ChecksumUtils.sha256(inputStream);
            if (!backupRecord.getChecksumSha256().equalsIgnoreCase(computed)) {
                throw BackupException.badRequest("Backup checksum mismatch");
            }
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to validate checksum: " + ex.getMessage());
        }
    }
}
