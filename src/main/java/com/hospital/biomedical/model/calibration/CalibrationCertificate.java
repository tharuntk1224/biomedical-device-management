package com.hospital.biomedical.model.calibration;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Formal Calibration Certificate generated exclusively upon successful verification.
 */
public class CalibrationCertificate implements Serializable {
    private String certificateNumber;
    private String deviceId;
    private String deviceName;
    private String assetNumber;
    private String technicianId;
    private String technicianName;
    private LocalDate issuedDate;
    private LocalDate validUntil;
    private String standardReference;

    public CalibrationCertificate() {
    }

    public CalibrationCertificate(String certificateNumber, String deviceId, String deviceName,
                                  String assetNumber, String technicianId, String technicianName,
                                  LocalDate issuedDate, LocalDate validUntil, String standardReference) {
        this.certificateNumber = certificateNumber;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.assetNumber = assetNumber;
        this.technicianId = technicianId;
        this.technicianName = technicianName;
        this.issuedDate = issuedDate;
        this.validUntil = validUntil;
        this.standardReference = standardReference;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAssetNumber() {
        return assetNumber;
    }

    public void setAssetNumber(String assetNumber) {
        this.assetNumber = assetNumber;
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        this.technicianId = technicianId;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getStandardReference() {
        return standardReference;
    }

    public void setStandardReference(String standardReference) {
        this.standardReference = standardReference;
    }

    @Override
    public String toString() {
        return String.format("Certificate [%s] for %s (%s) - Valid: %s to %s | Certified By: %s",
                certificateNumber, deviceName, assetNumber, issuedDate, validUntil, technicianName);
    }
}
