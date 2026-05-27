package com.example.daugia.backup.command;

import com.example.daugia.backup.dto.RestoreResult;

public interface RestoreCommand {
    RestoreResult execute();
    void rollback(Exception ex);
}
