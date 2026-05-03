package com.example.daugia.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AppException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
