package com.hospital.biomedical.model.calibration;

import com.hospital.biomedical.interfaces.Auditable;
import com.hospital.biomedical.interfaces.Reportable;
import com.hospital.biomedical.model.common.BaseEntity;
import com.hospital.biomedical.model.enums.CalibrationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a complete calibration verification test event.
 * Implements Auditable and Reportable.
 */
public class CalibrationRecord extends BaseEntity implements Auditable, Reportable {
    private String deviceId;
    private String technicianId;
    private LocalDate calibrationDate;
    private CalibrationStatus status;
    private List<CalibrationMeasurement> measurements;
    private CalibrationCertificate certificate;
    private String testEquipmentUsed;
    private String notes;
    private String failureReason;

    public CalibrationRecord() {
        super();
        this.measurements = new ArrayList<>();
        this.status = CalibrationStatus.IN_PROGRESS;
        this.calibrationDate = LocalDate.now();
    }

    public CalibrationRecord(String id, String deviceId, String technicianId,
                             LocalDate calibrationDate, String testEquipmentUsed, String notes) {
        super(id);
        setDeviceId(deviceId);
        setTechnicianId(technicianId);
        this.calibrationDate = calibrationDate != null ? calibrationDate : LocalDate.now();
        this.testEquipmentUsed = testEquipmentUsed != null ? testEquipmentUsed : "Biomedical Multimeter / Analyzer SIM-01";
        this.notes = notes != null ? notes : "";
        this.measurements = new ArrayList<>();
        this.status = CalibrationStatus.IN_PROGRESS;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be blank.");
        }
        this.deviceId = deviceId.trim();
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        if (technicianId == null || technicianId.trim().isEmpty()) {
            throw new IllegalArgumentException("Technician ID cannot be blank.");
        }
        this.technicianId = technicianId.trim();
    }

    public LocalDate getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(LocalDate calibrationDate) {
        this.calibrationDate = calibrationDate != null ? calibrationDate : LocalDate.now();
    }

    public CalibrationStatus getStatus() {
        return status;
    }

    public void setStatus(CalibrationStatus status) {
        this.status = status;
    }

    public List<CalibrationMeasurement> getMeasurements() {
        return new ArrayList<>(measurements);
    }

    public void addMeasurement(CalibrationMeasurement measurement) {
        if (measurement == null) {
            throw new IllegalArgumentException("Measurement cannot be null.");
        }
        this.measurements.add(measurement);
    }

    public void clearMeasurements() {
        this.measurements.clear();
    }

    public CalibrationCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(CalibrationCertificate certificate) {
        this.certificate = certificate;
    }

    public String getTestEquipmentUsed() {
        return testEquipmentUsed;
    }

    public void setTestEquipmentUsed(String testEquipmentUsed) {
        this.testEquipmentUsed = testEquipmentUsed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Evaluates calibration measurements.
     * Passes ONLY when there is at least one measurement and EVERY measurement passes.
     */
    public boolean evaluate() {
        if (measurements.isEmpty()) {
            this.status = CalibrationStatus.FAILED;
            this.failureReason = "Calibration record contains zero measurement parameters.";
            this.certificate = null;
            return false;
        }

        List<String> failedParams = new ArrayList<>();
        for (CalibrationMeasurement m : measurements) {
            m.recalculate();
            if (!m.isPassed()) {
                failedParams.add(String.format("%s (dev: %.3f > tol: %.3f)",
                        m.getParameterName(), m.getDeviation(), m.getAllowedTolerance()));
            }
        }

        if (failedParams.isEmpty()) {
            this.status = CalibrationStatus.PASSED;
            this.failureReason = null;
            return true;
        } else {
            this.status = CalibrationStatus.FAILED;
            this.failureReason = "Tolerance exceeded on parameters: " + String.join("; ", failedParams);
            this.certificate = null; // Strictly prevent certificate generation on failure
            return false;
        }
    }

    @Override
    public String getEntityIdentifier() {
        return getId();
    }

    @Override
    public String getEntityTypeName() {
        return "CalibrationRecord";
    }

    @Override
    public String toAuditSummary() {
        return String.format("Calibration: %s for Device %s, Result: %s, Tech: %s, MeasCount: %d",
                getId(), deviceId, status, technicianId, measurements.size());
    }

    @Override
    public String[] getReportHeaders() {
        return new String[]{"Calib ID", "Device ID", "Date", "Status", "Tech ID", "Cert No", "Measurements", "Failure Notes"};
    }

    @Override
    public String[] toReportRow() {
        return new String[]{
                getId(),
                getDeviceId(),
                getCalibrationDate().toString(),
                getStatus().name(),
                getTechnicianId(),
                getCertificate() != null ? getCertificate().getCertificateNumber() : "N/A",
                String.valueOf(measurements.size()),
                getFailureReason() != null ? getFailureReason() : "None"
        };
    }

    @Override
    public String toString() {
        return String.format("CalibrationRecord [%s] Device: %s | Date: %s | Status: %s | Cert: %s",
                getId(), deviceId, calibrationDate, status,
                certificate != null ? certificate.getCertificateNumber() : "None");
    }
}
