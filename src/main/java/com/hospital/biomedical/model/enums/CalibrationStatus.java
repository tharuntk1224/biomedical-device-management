package com.hospital.biomedical.model.enums;

/**
 * Result status of an individual calibration test session.
 */
public enum CalibrationStatus {
    PASSED("All calibration tolerance measurements met specifications"),
    FAILED("One or more measurements exceeded allowed tolerance"),
    IN_PROGRESS("Calibration verification underway"),
    CANCELLED("Calibration session aborted");

    private final String description;

    CalibrationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
