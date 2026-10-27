package com.hospital.biomedical.model.device;

import com.hospital.biomedical.model.enums.RiskLevel;

/**
 * Concrete device for diagnostic, monitoring, and imaging equipment.
 * Overrides calculateRiskScore and passesPreCheck for Polymorphism.
 */
public class DiagnosticDevice extends BiomedicalDevice {
    private boolean radiationEmitting;
    private boolean transducerIntegrityVerified;
    private String softwareFirmwareVersion;

    public DiagnosticDevice() {
        super();
        setCategory(DeviceCategory.DIAGNOSTIC);
        setRiskLevel(RiskLevel.MEDIUM);
        this.radiationEmitting = false;
        this.transducerIntegrityVerified = true;
        this.softwareFirmwareVersion = "v1.0";
    }

    public DiagnosticDevice(String id, String assetNumber, String serialNumber,
                            String name, String manufacturer, String modelNumber,
                            String department, RiskLevel riskLevel, int calibrationFrequencyDays,
                            boolean radiationEmitting, String softwareFirmwareVersion) {
        super(id, assetNumber, serialNumber, name, manufacturer, modelNumber,
                department, DeviceCategory.DIAGNOSTIC, riskLevel, true, calibrationFrequencyDays);
        this.radiationEmitting = radiationEmitting;
        this.transducerIntegrityVerified = true;
        this.softwareFirmwareVersion = softwareFirmwareVersion != null ? softwareFirmwareVersion : "v1.0";
    }

    public boolean isRadiationEmitting() {
        return radiationEmitting;
    }

    public void setRadiationEmitting(boolean radiationEmitting) {
        this.radiationEmitting = radiationEmitting;
    }

    public boolean isTransducerIntegrityVerified() {
        return transducerIntegrityVerified;
    }

    public void setTransducerIntegrityVerified(boolean transducerIntegrityVerified) {
        this.transducerIntegrityVerified = transducerIntegrityVerified;
    }

    public String getSoftwareFirmwareVersion() {
        return softwareFirmwareVersion;
    }

    public void setSoftwareFirmwareVersion(String softwareFirmwareVersion) {
        this.softwareFirmwareVersion = softwareFirmwareVersion;
    }

    @Override
    public double calculateRiskScore() {
        // Base severity * 1.8 + radiation factor
        double score = getRiskLevel().getSeverityScore() * 1.8;
        if (radiationEmitting) {
            score += 1.5; // Additional regulatory and safety inspection weight
        }
        if (!transducerIntegrityVerified) {
            score += 2.0;
        }
        return Math.round(score * 100.0) / 100.0;
    }

    @Override
    public boolean passesPreCheck() {
        return transducerIntegrityVerified;
    }
}
