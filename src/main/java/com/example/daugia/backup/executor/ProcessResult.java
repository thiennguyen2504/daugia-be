package com.example.daugia.backup.executor;

public record ProcessResult(int exitCode, String errorOutput) {
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
