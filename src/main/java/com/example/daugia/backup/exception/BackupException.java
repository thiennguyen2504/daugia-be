package com.example.daugia.backup.exception;

import com.example.daugia.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class BackupException extends AppException {

    public BackupException(String message, HttpStatus status) {
        super(message, status);
    }

    public static BackupException notFound(String message) {
        return new BackupException(message, HttpStatus.NOT_FOUND);
    }

    public static BackupException badRequest(String message) {
        return new BackupException(message, HttpStatus.BAD_REQUEST);
    }

    public static BackupException serverError(String message) {
        return new BackupException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
