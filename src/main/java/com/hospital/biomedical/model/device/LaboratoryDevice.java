package com.hospital.biomedical.model.device;

import com.hospital.biomedical.model.enums.RiskLevel;

/**
 * Concrete device for in-vitro diagnostic, centrifuge, and clinical analyzers.
 * Overrides calculateRiskScore and passesPreCheck for Polymorphism.
 */
public class LaboratoryDevice extends BiomedicalDevice {
    private boolean requiresTemperatureControl;
    private double operatingTemperatureCelsius;
    private int biosafetyLevel;

    public LaboratoryDevice() {
        super();
        setCategory(DeviceCategory.LABORATORY);
        setRiskLevel(RiskLevel.LOW);
        this.requiresTemperatureControl = false;
        this.operatingTemperatureCelsius = 22.0;
        this.biosafetyLevel = 1;
    }

    public LaboratoryDevice(String id, String assetNumber, String serialNumber,
                            String name, String manufacturer, String modelNumber,
                            String department, RiskLevel riskLevel, int calibrationFrequencyDays,
                            boolean requiresTemperatureControl, double operatingTemperatureCelsius,
                            int biosafetyLevel) {
        super(id, assetNumber, serialNumber, name, manufacturer, modelNumber,
                department, DeviceCategory.LABORATORY, riskLevel, true, calibrationFrequencyDays);
        this.requiresTemperatureControl = requiresTemperatureControl;
        this.operatingTemperatureCelsius = operatingTemperatureCelsius;
        setBiosafetyLevel(biosafetyLevel);
    }

    public boolean isRequiresTemperatureControl() {
        return requiresTemperatureControl;
    }

    public void setRequiresTemperatureControl(boolean requiresTemperatureControl) {
        this.requiresTemperatureControl = requiresTemperatureControl;
    }

    public double getOperatingTemperatureCelsius() {
        return operatingTemperatureCelsius;
    }

    public void setOperatingTemperatureCelsius(double operatingTemperatureCelsius) {
        this.operatingTemperatureCelsius = operatingTemperatureCelsius;
    }

    public int getBiosafetyLevel() {
        return biosafetyLevel;
    }

    public void setBiosafetyLevel(int biosafetyLevel) {
        if (biosafetyLevel < 1 || biosafetyLevel > 4) {
            throw new IllegalArgumentException("Biosafety level must be between 1 and 4.");
        }
        this.biosafetyLevel = biosafetyLevel;
    }

    @Override
    public double calculateRiskScore() {
        // Base severity * 1.2 + biosafety multiplier
        double score = (getRiskLevel().getSeverityScore() * 1.2) + (biosafetyLevel * 0.75);
        if (requiresTemperatureControl && (operatingTemperatureCelsius < 2.0 || operatingTemperatureCelsius > 8.0)) {
            score += 1.0;
        }
        return Math.round(score * 100.0) / 100.0;
    }

    @Override
    public boolean passesPreCheck() {
        // Laboratory devices pass if temperature criteria is satisfied
        if (requiresTemperatureControl) {
            return operatingTemperatureCelsius >= 0.0 && operatingTemperatureCelsius <= 25.0;
        }
        return true;
    }
}
