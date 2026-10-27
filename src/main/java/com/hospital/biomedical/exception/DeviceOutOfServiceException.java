package com.hospital.biomedical.exception;

public class DeviceOutOfServiceException extends IllegalStateException {
    public DeviceOutOfServiceException(String message) {
        super(message);
    }
}
