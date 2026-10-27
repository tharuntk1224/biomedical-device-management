package com.hospital.biomedical.model.enums;

/**
 * Operational lifecycle statuses for biomedical devices.
 */
public enum DeviceStatus {
    OPERATIONAL("Operational and approved for clinical use"),
    UNDER_MAINTENANCE("Currently undergoing maintenance or repair"),
    CALIBRATION_PENDING("Awaiting required calibration verification"),
    OUT_OF_SERVICE("Unsafe or failed calibration; prohibited from clinical use"),
    RETIRED("Decommissioned permanently and archived");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
