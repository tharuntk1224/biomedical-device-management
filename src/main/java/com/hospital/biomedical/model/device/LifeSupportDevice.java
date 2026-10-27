package com.hospital.biomedical.model.device;

import com.hospital.biomedical.model.enums.RiskLevel;

/**
 * Concrete device for critical patient life-sustaining equipment.
 * Overrides calculateRiskScore and passesPreCheck for Polymorphism.
 */
public class LifeSupportDevice extends BiomedicalDevice {
    private int batteryBackupMinutes;
    private boolean redundantAlarmsOperational;
    private int patientConnectionCount;

    public LifeSupportDevice() {
        super();
        setCategory(DeviceCategory.LIFE_SUPPORT);
        setRiskLevel(RiskLevel.CRITICAL);
        this.batteryBackupMinutes = 120;
        this.redundantAlarmsOperational = true;
    }

    public LifeSupportDevice(String id, String assetNumber, String serialNumber,
                             String name, String manufacturer, String modelNumber,
                             String department, int calibrationFrequencyDays,
                             int batteryBackupMinutes, boolean redundantAlarmsOperational) {
        super(id, assetNumber, serialNumber, name, manufacturer, modelNumber,
                department, DeviceCategory.LIFE_SUPPORT, RiskLevel.CRITICAL, true, calibrationFrequencyDays);
        setBatteryBackupMinutes(batteryBackupMinutes);
        this.redundantAlarmsOperational = redundantAlarmsOperational;
        this.patientConnectionCount = 1;
    }

    public int getBatteryBackupMinutes() {
        return batteryBackupMinutes;
    }

    public void setBatteryBackupMinutes(int batteryBackupMinutes) {
        if (batteryBackupMinutes < 0) {
            throw new IllegalArgumentException("Battery backup minutes cannot be negative.");
        }
        this.batteryBackupMinutes = batteryBackupMinutes;
    }

    public boolean isRedundantAlarmsOperational() {
        return redundantAlarmsOperational;
    }

    public void setRedundantAlarmsOperational(boolean redundantAlarmsOperational) {
        this.redundantAlarmsOperational = redundantAlarmsOperational;
    }

    public int getPatientConnectionCount() {
        return patientConnectionCount;
    }

    public void setPatientConnectionCount(int patientConnectionCount) {
        this.patientConnectionCount = Math.max(0, patientConnectionCount);
    }

    @Override
    public double calculateRiskScore() {
        // High risk base for life support: Base severity (4.0) * 2.5 + alarm penalty + battery buffer penalty
        double score = getRiskLevel().getSeverityScore() * 2.5;
        if (!redundantAlarmsOperational) {
            score += 3.0; // Significant penalty if redundant alarms fail
        }
        if (batteryBackupMinutes < 60) {
            score += 2.0; // Warning for low battery backup
        }
        return Math.round(score * 100.0) / 100.0;
    }

    @Override
    public boolean passesPreCheck() {
        // Life support must have operational alarms and minimum 30 minutes battery
        return redundantAlarmsOperational && batteryBackupMinutes >= 30;
    }
}
