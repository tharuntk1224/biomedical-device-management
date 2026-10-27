package com.hospital.biomedical.model.enums;

/**
 * Types of biomedical maintenance activities.
 */
public enum MaintenanceType {
    PREVENTIVE("Scheduled inspection, cleaning, lubrication, and parts replacement"),
    CORRECTIVE("Remediation of malfunctions, breakdowns, or performance drift"),
    EMERGENCY("Immediate intervention on critical failure or clinical distress");

    private final String description;

    MaintenanceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
