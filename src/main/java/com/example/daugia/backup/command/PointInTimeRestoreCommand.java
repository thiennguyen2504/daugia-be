package com.example.daugia.backup.command;

import com.example.daugia.backup.dto.RestoreResult;
import com.example.daugia.backup.dto.RestoreStatus;
import com.example.daugia.backup.exception.BackupException;
import com.example.daugia.backup.executor.BackupProcessExecutor;
import com.example.daugia.backup.executor.ProcessResult;
import com.example.daugia.backup.storage.BackupStorageProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class PointInTimeRestoreCommand implements RestoreCommand {

    private final String restoreId;
    private final RestoreCommand fullRestoreCommand;
    private final BackupStorageProvider storageProvider;
    private final BackupProcessExecutor processExecutor;
    private final List<Path> walFiles;
    private final LocalDateTime targetTime;
    private final List<String> replayCommand;

    public PointInTimeRestoreCommand(String restoreId,
                                     RestoreCommand fullRestoreCommand,
                                     BackupStorageProvider storageProvider,
                                     BackupProcessExecutor processExecutor,
                                     List<Path> walFiles,
                                     LocalDateTime targetTime,
                                     List<String> replayCommand) {
        this.restoreId = restoreId;
        this.fullRestoreCommand = fullRestoreCommand;
        this.storageProvider = storageProvider;
        this.processExecutor = processExecutor;
        this.walFiles = walFiles;
        this.targetTime = targetTime;
        this.replayCommand = replayCommand;
    }

    @Override
    public RestoreResult execute() {
        fullRestoreCommand.execute();
        replayWalFiles();
        return new RestoreResult(restoreId, RestoreStatus.SUCCESS, "Point-in-time restore completed", null);
    }

    @Override
    public void rollback(Exception ex) {
        fullRestoreCommand.rollback(ex);
    }

    private void replayWalFiles() {
        if (walFiles == null || walFiles.isEmpty()) {
            return;
        }

        List<Path> ordered = walFiles.stream()
                .sorted(Comparator.comparing(Path::toString))
                .collect(Collectors.toList());

        for (Path walFile : ordered) {
            if (!storageProvider.exists(walFile)) {
                continue;
            }
            applyWalFile(walFile);
        }
    }

    private void applyWalFile(Path walFile) {
        try (InputStream fileIn = storageProvider.openInputStream(walFile);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn)) {
            ProcessResult result = processExecutor.executeWithInput(replayCommand, null, gzipIn);
            if (!result.isSuccess()) {
                throw BackupException.serverError("WAL replay failed: " + result.errorOutput());
            }
        } catch (IOException ex) {
            throw BackupException.serverError("Failed to apply WAL file: " + ex.getMessage());
        }
    }
}
