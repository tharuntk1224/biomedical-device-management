package com.hospital.biomedical.model.maintenance;

import com.hospital.biomedical.model.enums.MaintenanceType;

/**
 * Concrete Corrective Maintenance implementation.
 * Repairing functional defects, part breakdowns, or sensor drift.
 */
public class CorrectiveMaintenance extends AbstractMaintenanceRecord {
    private String rootCause;
    private String failureCategory;

    public CorrectiveMaintenance() {
        super();
        setMaintenanceType(MaintenanceType.CORRECTIVE);
    }

    public CorrectiveMaintenance(String id, String deviceId, String technicianId,
                                String workDescription, String rootCause, String failureCategory) {
        super(id, deviceId, technicianId, MaintenanceType.CORRECTIVE, workDescription);
        this.rootCause = rootCause != null ? rootCause : "Component Wear";
        this.failureCategory = failureCategory != null ? failureCategory : "Electrical";
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    @Override
    public boolean requiresRecalibration() {
        // Corrective repairs almost always alter device physical parameters, demanding recalibration
        return true;
    }

    @Override
    public double calculateUrgencyMultiplier() {
        return 1.8;
    }
}
