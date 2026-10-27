package com.hospital.biomedical.exception;

public class UnauthorizedAccessException extends SecurityException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
