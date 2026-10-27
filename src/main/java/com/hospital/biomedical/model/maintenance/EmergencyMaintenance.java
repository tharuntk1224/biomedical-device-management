package com.hospital.biomedical.model.maintenance;

import com.hospital.biomedical.model.enums.MaintenanceType;

/**
 * Concrete Emergency Maintenance implementation.
 * Urgent intervention on sudden clinical equipment faults.
 */
public class EmergencyMaintenance extends AbstractMaintenanceRecord {
    private String clinicalImpact;
    private boolean patientInvolved;
    private String reportedByStaff;

    public EmergencyMaintenance() {
        super();
        setMaintenanceType(MaintenanceType.EMERGENCY);
        this.patientInvolved = false;
    }

    public EmergencyMaintenance(String id, String deviceId, String technicianId,
                                String workDescription, String clinicalImpact,
                                boolean patientInvolved, String reportedByStaff) {
        super(id, deviceId, technicianId, MaintenanceType.EMERGENCY, workDescription);
        this.clinicalImpact = clinicalImpact != null ? clinicalImpact : "Clinical procedure interrupted";
        this.patientInvolved = patientInvolved;
        this.reportedByStaff = reportedByStaff != null ? reportedByStaff : "ICU Charge Nurse";
    }

    public String getClinicalImpact() {
        return clinicalImpact;
    }

    public void setClinicalImpact(String clinicalImpact) {
        this.clinicalImpact = clinicalImpact;
    }

    public boolean isPatientInvolved() {
        return patientInvolved;
    }

    public void setPatientInvolved(boolean patientInvolved) {
        this.patientInvolved = patientInvolved;
    }

    public String getReportedByStaff() {
        return reportedByStaff;
    }

    public void setReportedByStaff(String reportedByStaff) {
        this.reportedByStaff = reportedByStaff;
    }

    @Override
    public boolean requiresRecalibration() {
        // Emergency interventions strictly mandate comprehensive safety recalibration before returning to service
        return true;
    }

    @Override
    public double calculateUrgencyMultiplier() {
        return patientInvolved ? 3.5 : 2.5;
    }
}
