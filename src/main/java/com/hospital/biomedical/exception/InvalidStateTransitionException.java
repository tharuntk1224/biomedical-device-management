package com.hospital.biomedical.exception;

public class InvalidStateTransitionException extends IllegalStateException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
