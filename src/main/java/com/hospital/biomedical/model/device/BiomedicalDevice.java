package com.hospital.biomedical.model.device;

import com.hospital.biomedical.interfaces.Auditable;
import com.hospital.biomedical.interfaces.Calibratable;
import com.hospital.biomedical.interfaces.Maintainable;
import com.hospital.biomedical.interfaces.Reportable;
import com.hospital.biomedical.model.common.BaseEntity;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.RiskLevel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Abstract root of biomedical equipment hierarchy.
 * Demonstrates Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class BiomedicalDevice extends BaseEntity implements Calibratable, Maintainable, Auditable, Reportable {
    private String assetNumber;
    private String serialNumber;
    private String name;
    private String manufacturer;
    private String modelNumber;
    private String department;
    private DeviceCategory category;
    private String assignedTechnicianId;
    private DeviceStatus deviceStatus;
    private RiskLevel riskLevel;
    private boolean calibrationRequired;
    private int calibrationFrequencyDays;
    private LocalDate lastCalibrationDate;
    private LocalDate nextCalibrationDate;

    public BiomedicalDevice() {
        super();
        this.deviceStatus = DeviceStatus.CALIBRATION_PENDING;
        this.riskLevel = RiskLevel.MEDIUM;
        this.calibrationRequired = true;
        this.calibrationFrequencyDays = 180;
    }

    public BiomedicalDevice(String id, String assetNumber, String serialNumber,
                            String name, String manufacturer, String modelNumber,
                            String department, DeviceCategory category,
                            RiskLevel riskLevel, boolean calibrationRequired,
                            int calibrationFrequencyDays) {
        super(id);
        setAssetNumber(assetNumber);
        setSerialNumber(serialNumber);
        setName(name);
        setManufacturer(manufacturer);
        setModelNumber(modelNumber);
        setDepartment(department);
        setCategory(category);
        setRiskLevel(riskLevel);
        setCalibrationRequired(calibrationRequired);
        setCalibrationFrequencyDays(calibrationFrequencyDays);
        this.deviceStatus = calibrationRequired ? DeviceStatus.CALIBRATION_PENDING : DeviceStatus.OPERATIONAL;
        this.assignedTechnicianId = "UNASSIGNED";
    }

    // Encapsulated Getters & Setters with Business Validations

    public String getDeviceId() {
        return getId();
    }

    public void setDeviceId(String deviceId) {
        setId(deviceId);
    }

    public String getAssetNumber() {
        return assetNumber;
    }

    public void setAssetNumber(String assetNumber) {
        if (assetNumber == null || assetNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset number cannot be blank.");
        }
        this.assetNumber = assetNumber.trim().toUpperCase();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Serial number cannot be blank.");
        }
        this.serialNumber = serialNumber.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be blank.");
        }
        this.name = name.trim();
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer != null ? manufacturer.trim() : "Unknown OEM";
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber != null ? modelNumber.trim() : "Standard";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : "Biomedical";
    }

    public DeviceCategory getCategory() {
        return category;
    }

    public void setCategory(DeviceCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Device category cannot be null.");
        }
        this.category = category;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = (assignedTechnicianId != null && !assignedTechnicianId.trim().isEmpty())
                ? assignedTechnicianId.trim() : "UNASSIGNED";
    }

    @Override
    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        if (deviceStatus == null) {
            throw new IllegalArgumentException("Device status cannot be null.");
        }
        // Safety lock: Retired devices cannot be switched back through casual setter
        if (this.deviceStatus == DeviceStatus.RETIRED && deviceStatus != DeviceStatus.RETIRED) {
            throw new IllegalStateException("Retired devices cannot be reactivated directly.");
        }
        this.deviceStatus = deviceStatus;
        touchUpdated();
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        if (riskLevel == null) {
            throw new IllegalArgumentException("Risk level cannot be null.");
        }
        this.riskLevel = riskLevel;
    }

    @Override
    public boolean isCalibrationRequired() {
        return calibrationRequired;
    }

    public void setCalibrationRequired(boolean calibrationRequired) {
        this.calibrationRequired = calibrationRequired;
    }

    @Override
    public int getCalibrationFrequencyDays() {
        return calibrationFrequencyDays;
    }

    public void setCalibrationFrequencyDays(int calibrationFrequencyDays) {
        if (this.calibrationRequired && calibrationFrequencyDays <= 0) {
            throw new IllegalArgumentException("Calibration frequency must be greater than 0 days when calibration is required.");
        }
        this.calibrationFrequencyDays = calibrationFrequencyDays;
    }

    @Override
    public LocalDate getLastCalibrationDate() {
        return lastCalibrationDate;
    }

    public void setLastCalibrationDate(LocalDate lastCalibrationDate) {
        this.lastCalibrationDate = lastCalibrationDate;
    }

    @Override
    public LocalDate getNextCalibrationDate() {
        return nextCalibrationDate;
    }

    public void setNextCalibrationDate(LocalDate nextCalibrationDate) {
        this.nextCalibrationDate = nextCalibrationDate;
    }

    // Polymorphic Abstract Methods
    public abstract double calculateRiskScore();
    public abstract boolean passesPreCheck();

    // Calibratable Interface Implementation
    @Override
    public void recordCalibration(LocalDate calibrationDate, boolean passed) {
        if (this.deviceStatus == DeviceStatus.RETIRED) {
            throw new IllegalStateException("Safety Violation: Cannot calibrate a retired device (" + getId() + ").");
        }
        Objects.requireNonNull(calibrationDate, "Calibration date cannot be null.");
        this.lastCalibrationDate = calibrationDate;
        touchUpdated();

        if (passed) {
            this.nextCalibrationDate = calibrationDate.plusDays(this.calibrationFrequencyDays);
            this.deviceStatus = DeviceStatus.OPERATIONAL;
        } else {
            // Failed calibration locks the device out of clinical operation
            this.deviceStatus = DeviceStatus.OUT_OF_SERVICE;
            // The next calibration date remains overdue or today until recalibrated
            this.nextCalibrationDate = calibrationDate;
        }
    }

    @Override
    public CalibrationDueStatus getCalibrationDueStatus(LocalDate referenceDate) {
        if (!this.calibrationRequired) {
            return CalibrationDueStatus.NOT_REQUIRED;
        }
        if (this.nextCalibrationDate == null) {
            return CalibrationDueStatus.OVERDUE; // Never calibrated but required
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(ref, this.nextCalibrationDate);

        if (daysUntilDue < 0) {
            return CalibrationDueStatus.OVERDUE;
        } else if (daysUntilDue == 0) {
            return CalibrationDueStatus.DUE_TODAY;
        } else if (daysUntilDue <= 7) {
            return CalibrationDueStatus.DUE_SOON;
        } else {
            return CalibrationDueStatus.VALID;
        }
    }

    // Maintainable Interface Implementation
    @Override
    public void startMaintenance() {
        if (this.deviceStatus == DeviceStatus.RETIRED) {
            throw new IllegalStateException("Cannot service retired equipment.");
        }
        this.deviceStatus = DeviceStatus.UNDER_MAINTENANCE;
        touchUpdated();
    }

    @Override
    public void completeMaintenance(boolean requireRecalibration) {
        if (this.deviceStatus == DeviceStatus.RETIRED) {
            return;
        }
        touchUpdated();
        if (requireRecalibration || this.calibrationRequired && (this.nextCalibrationDate == null || this.nextCalibrationDate.isBefore(LocalDate.now()))) {
            this.deviceStatus = DeviceStatus.CALIBRATION_PENDING;
        } else {
            this.deviceStatus = DeviceStatus.OPERATIONAL;
        }
    }

    @Override
    public boolean canBeOperated() {
        return this.deviceStatus == DeviceStatus.OPERATIONAL;
    }

    // Auditable Implementation
    @Override
    public String getEntityIdentifier() {
        return getId();
    }

    @Override
    public String getEntityTypeName() {
        return "BiomedicalDevice";
    }

    @Override
    public String toAuditSummary() {
        return String.format("Device: %s [%s] (%s - %s), Dept: %s, Status: %s, Risk: %s",
                getName(), getId(), getManufacturer(), getModelNumber(), getDepartment(), getDeviceStatus(), getRiskLevel());
    }

    // Reportable Implementation
    @Override
    public String[] getReportHeaders() {
        return new String[]{"Device ID", "Asset No", "Name", "Category", "Department", "Status", "Risk Level", "Calib Due", "Next Calib Date"};
    }

    @Override
    public String[] toReportRow() {
        return new String[]{
                getId(),
                getAssetNumber(),
                getName(),
                getCategory().name(),
                getDepartment(),
                getDeviceStatus().name(),
                getRiskLevel().name(),
                getCalibrationDueStatus(LocalDate.now()).name(),
                getNextCalibrationDate() != null ? getNextCalibrationDate().toString() : "None"
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Dept: %s | Status: %s | Risk: %s",
                getId(), getName(), getAssetNumber(), getDepartment(), getDeviceStatus(), getRiskLevel());
    }
}
