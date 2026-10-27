package com.hospital.biomedical.model.enums;

/**
 * Status of device calibration schedule relative to the current date.
 */
public enum CalibrationDueStatus {
    VALID("Calibration is current and within tolerance window"),
    DUE_SOON("Calibration is due within the next 7 days"),
    DUE_TODAY("Calibration is due today"),
    OVERDUE("Calibration date has passed without verification"),
    NOT_REQUIRED("Device does not require periodic calibration");

    private final String description;

    CalibrationDueStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
