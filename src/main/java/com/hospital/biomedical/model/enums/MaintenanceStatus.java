package com.hospital.biomedical.model.enums;

/**
 * State machine stages for maintenance lifecycle.
 */
public enum MaintenanceStatus {
    REQUESTED("Maintenance logged and pending engineering approval"),
    APPROVED("Maintenance request vetted and authorized"),
    SCHEDULED("Work order assigned to technician with planned date"),
    IN_PROGRESS("Technician actively servicing the equipment"),
    COMPLETED("Work finished, parts replaced, checklist verified"),
    CANCELLED("Work order terminated before completion");

    private final String description;

    MaintenanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
