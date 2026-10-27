package com.hospital.biomedical.exception;

public class DuplicateEntityException extends IllegalArgumentException {
    public DuplicateEntityException(String message) {
        super(message);
    }
}
