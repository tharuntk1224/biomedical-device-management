package com.hospital.biomedical.service;

import com.hospital.biomedical.exception.CalibrationToleranceExceededException;
import com.hospital.biomedical.exception.DeviceNotFoundException;
import com.hospital.biomedical.exception.DeviceOutOfServiceException;
import com.hospital.biomedical.model.calibration.CalibrationCertificate;
import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.CalibrationStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.UserRole;
import com.hospital.biomedical.repository.CalibrationRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.util.IdGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Service orchestrating calibration tests, tolerance verification, certification,
 * and safety lockouts.
 */
public class CalibrationService {
    private final CalibrationRepository calibrationRepository;
    private final DeviceRepository deviceRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public CalibrationService(CalibrationRepository calibrationRepository, DeviceRepository deviceRepository,
                              AuthService authService, AuditService auditService) {
        this.calibrationRepository = calibrationRepository;
        this.deviceRepository = deviceRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    public synchronized CalibrationRecord performCalibration(String deviceId, String technicianId,
                                                             List<CalibrationMeasurement> measurements,
                                                             String equipmentUsed, String notes) {
        return performCalibration(deviceId, technicianId, measurements, equipmentUsed, notes, LocalDate.now());
    }

    public synchronized CalibrationRecord performCalibration(String deviceId, String technicianId,
                                                             List<CalibrationMeasurement> measurements,
                                                             String equipmentUsed, String notes,
                                                             LocalDate calibrationDate) {
        authService.checkPermission(UserRole.TECHNICIAN, UserRole.ADMIN);

        Objects.requireNonNull(deviceId, "Device ID cannot be null.");
        BiomedicalDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));

        if (device.getDeviceStatus() == DeviceStatus.RETIRED) {
            throw new DeviceOutOfServiceException("Safety Lock: Cannot calibrate retired device (" + deviceId + ").");
        }

        LocalDate calDate = calibrationDate != null ? calibrationDate : LocalDate.now();
        String calId = IdGenerator.nextCalibrationId();
        CalibrationRecord record = new CalibrationRecord(calId, device.getId(), technicianId,
                calDate, equipmentUsed, notes);

        if (measurements != null) {
            for (CalibrationMeasurement m : measurements) {
                record.addMeasurement(m);
            }
        }

        boolean passed = record.evaluate();

        if (passed) {
            // Successful calibration
            String certNo = IdGenerator.nextCertificateNumber();
            LocalDate validUntil = calDate.plusDays(device.getCalibrationFrequencyDays());
            String techName = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : technicianId;

            CalibrationCertificate cert = new CalibrationCertificate(
                    certNo, device.getId(), device.getName(), device.getAssetNumber(),
                    technicianId, techName, calDate, validUntil,
                    "Hospital Biomedical Safety & Verification Standard SIM-01"
            );
            record.setCertificate(cert);

            // Update device status and next due date
            device.recordCalibration(calDate, true);
            deviceRepository.save(device);
            calibrationRepository.save(record);

            String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : technicianId;
            auditService.log(username, "CALIBRATION_PASSED", "CalibrationRecord", record.getId(),
                    String.format("Device %s passed calibration. Cert #%s issued. Valid until: %s",
                            device.getId(), certNo, validUntil));
        } else {
            // Failed calibration locks device out of service
            device.recordCalibration(calDate, false);
            deviceRepository.save(device);
            calibrationRepository.save(record);

            String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : technicianId;
            auditService.log(username, "CALIBRATION_FAILED", "CalibrationRecord", record.getId(),
                    String.format("Device %s FAILED calibration! Device marked OUT_OF_SERVICE. Reason: %s",
                            device.getId(), record.getFailureReason()));
        }

        return record;
    }

    public synchronized List<CalibrationRecord> getCalibrationHistory(String deviceId) {
        return calibrationRepository.findByDeviceId(deviceId);
    }

    public synchronized List<CalibrationRecord> getAllCalibrations() {
        return calibrationRepository.findAll();
    }

    public synchronized CalibrationDueStatus checkDueStatus(String deviceId) {
        BiomedicalDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));
        return device.getCalibrationDueStatus(LocalDate.now());
    }
}
