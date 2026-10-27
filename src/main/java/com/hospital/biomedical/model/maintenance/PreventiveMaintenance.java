package com.hospital.biomedical.model.maintenance;

import com.hospital.biomedical.model.enums.MaintenanceType;

/**
 * Concrete Preventive Maintenance implementation.
 * Routine inspection, cleaning, filter replacements, and mechanical alignment.
 */
public class PreventiveMaintenance extends AbstractMaintenanceRecord {
    private int periodicCycleMonths;
    private boolean lubricationDone;
    private boolean electricalSafetyTestPassed;

    public PreventiveMaintenance() {
        super();
        setMaintenanceType(MaintenanceType.PREVENTIVE);
        this.periodicCycleMonths = 6;
        this.electricalSafetyTestPassed = true;
    }

    public PreventiveMaintenance(String id, String deviceId, String technicianId,
                                 String workDescription, int periodicCycleMonths) {
        super(id, deviceId, technicianId, MaintenanceType.PREVENTIVE, workDescription);
        this.periodicCycleMonths = periodicCycleMonths;
        this.electricalSafetyTestPassed = true;
    }

    public int getPeriodicCycleMonths() {
        return periodicCycleMonths;
    }

    public void setPeriodicCycleMonths(int periodicCycleMonths) {
        this.periodicCycleMonths = periodicCycleMonths;
    }

    public boolean isLubricationDone() {
        return lubricationDone;
    }

    public void setLubricationDone(boolean lubricationDone) {
        this.lubricationDone = lubricationDone;
    }

    public boolean isElectricalSafetyTestPassed() {
        return electricalSafetyTestPassed;
    }

    public void setElectricalSafetyTestPassed(boolean electricalSafetyTestPassed) {
        this.electricalSafetyTestPassed = electricalSafetyTestPassed;
    }

    @Override
    public boolean requiresRecalibration() {
        // Preventive maintenance requires recalibration only if critical sensing parts were replaced
        return !getPartsReplaced().isEmpty();
    }

    @Override
    public double calculateUrgencyMultiplier() {
        return 1.0; // Standard priority
    }
}
