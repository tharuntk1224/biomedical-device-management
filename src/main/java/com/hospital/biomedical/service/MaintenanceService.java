package com.hospital.biomedical.service;

import com.hospital.biomedical.exception.DeviceNotFoundException;
import com.hospital.biomedical.exception.InvalidStateTransitionException;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.enums.MaintenanceType;
import com.hospital.biomedical.model.enums.UserRole;
import com.hospital.biomedical.model.maintenance.*;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.MaintenanceRepository;
import com.hospital.biomedical.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Service managing biomedical maintenance requests, work orders, state transitions,
 * and post-service safety status verification.
 */
public class MaintenanceService {
    private final MaintenanceRepository maintenanceRepository;
    private final DeviceRepository deviceRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, DeviceRepository deviceRepository,
                              AuthService authService, AuditService auditService) {
        this.maintenanceRepository = maintenanceRepository;
        this.deviceRepository = deviceRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    public synchronized MaintenanceRequest createRequest(String deviceId, MaintenanceType type, String description) {
        authService.checkPermission(UserRole.STAFF, UserRole.ADMIN, UserRole.TECHNICIAN);
        Objects.requireNonNull(deviceId, "Device ID cannot be null.");

        BiomedicalDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));

        if (device.getDeviceStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("Cannot create maintenance request for retired device.");
        }

        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "staff";
        String reqId = IdGenerator.nextRequestId();
        MaintenanceRequest request = new MaintenanceRequest(reqId, device.getId(), username, type, description);
        maintenanceRepository.saveRequest(request);

        auditService.log(username, "MAINTENANCE_REQUEST_CREATED", "MaintenanceRequest", reqId,
                String.format("Created %s request for device %s: %s", type, device.getId(), description));
        return request;
    }

    public synchronized void approveAndScheduleRequest(String requestId, String assignedTechId, LocalDateTime scheduledTime) {
        authService.checkPermission(UserRole.ADMIN);
        MaintenanceRequest request = maintenanceRepository.findRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found: " + requestId));

        if (request.getStatus() != MaintenanceStatus.REQUESTED) {
            throw new InvalidStateTransitionException("Cannot approve request in status: " + request.getStatus());
        }

        request.setStatus(MaintenanceStatus.SCHEDULED);
        request.setAssignedTechnicianId(assignedTechId != null ? assignedTechId : "UNASSIGNED");
        request.setScheduledAt(scheduledTime != null ? scheduledTime : LocalDateTime.now());

        maintenanceRepository.saveRequest(request);
        String username = authService.getCurrentUser().getUsername();
        auditService.log(username, "MAINTENANCE_SCHEDULED", "MaintenanceRequest", request.getId(),
                "Approved and scheduled for tech: " + request.getAssignedTechnicianId());
    }

    public synchronized AbstractMaintenanceRecord startMaintenance(String requestId, String technicianId) {
        authService.checkPermission(UserRole.TECHNICIAN, UserRole.ADMIN);
        MaintenanceRequest request = maintenanceRepository.findRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found: " + requestId));

        if (request.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Maintenance is already in progress.");
        }
        if (request.getStatus() == MaintenanceStatus.COMPLETED || request.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot start maintenance on a closed/cancelled request.");
        }

        BiomedicalDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + request.getDeviceId()));

        if (device.getDeviceStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("Cannot service retired equipment.");
        }

        // Set device to UNDER_MAINTENANCE
        device.startMaintenance();
        deviceRepository.save(device);

        String recordId = IdGenerator.nextMaintenanceId();
        AbstractMaintenanceRecord record;
        MaintenanceType type = request.getRequestedType();

        if (type == MaintenanceType.EMERGENCY) {
            record = new EmergencyMaintenance(recordId, device.getId(), technicianId,
                    request.getIssueDescription(), "Clinical service alert", false, request.getRequestedByUsername());
        } else if (type == MaintenanceType.CORRECTIVE) {
            record = new CorrectiveMaintenance(recordId, device.getId(), technicianId,
                    request.getIssueDescription(), "Clinical defect report", "General Hardware");
        } else {
            record = new PreventiveMaintenance(recordId, device.getId(), technicianId,
                    request.getIssueDescription(), 6);
        }

        record.setMaintenanceStatus(MaintenanceStatus.IN_PROGRESS);
        record.setStartedDateTime(LocalDateTime.now());
        record.setScheduledDateTime(request.getScheduledAt());

        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        request.setMaintenanceRecordId(record.getId());
        request.setAssignedTechnicianId(technicianId);

        maintenanceRepository.saveRecord(record);
        maintenanceRepository.saveRequest(request);

        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : technicianId;
        auditService.log(username, "MAINTENANCE_STARTED", "MaintenanceRecord", record.getId(),
                "Started maintenance on device " + device.getId() + " by " + technicianId);

        return record;
    }

    public synchronized void completeMaintenance(String recordId, BigDecimal cost, double downtimeHours,
                                                List<String> partsReplaced, List<String> checklistItems,
                                                String resolutionNotes) {
        authService.checkPermission(UserRole.TECHNICIAN, UserRole.ADMIN);
        AbstractMaintenanceRecord record = maintenanceRepository.findRecordById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found: " + recordId));

        if (record.getMaintenanceStatus() == MaintenanceStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Completed maintenance task cannot be reopened without explicit authorized workflow.");
        }

        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maintenance cost cannot be negative.");
        }
        if (downtimeHours < 0) {
            throw new IllegalArgumentException("Equipment downtime hours cannot be negative.");
        }

        record.setCost(cost);
        record.setDowntimeHours(downtimeHours);
        if (partsReplaced != null) record.setPartsReplaced(partsReplaced);
        if (checklistItems != null) record.setChecklistItems(checklistItems);
        record.setResolutionNotes(resolutionNotes != null ? resolutionNotes : "Work completed successfully.");
        record.setMaintenanceStatus(MaintenanceStatus.COMPLETED);
        record.setCompletedDateTime(LocalDateTime.now());

        // Update corresponding request
        for (MaintenanceRequest req : maintenanceRepository.findAllRequests()) {
            if (record.getId().equals(req.getMaintenanceRecordId())) {
                req.setStatus(MaintenanceStatus.COMPLETED);
                maintenanceRepository.saveRequest(req);
                break;
            }
        }

        // SAFETY ENFORCEMENT:
        // Do not automatically mark a failed-calibration device operational simply because maintenance is completed!
        BiomedicalDevice device = deviceRepository.findById(record.getDeviceId())
                .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + record.getDeviceId()));

        boolean requireRecalibration = record.requiresRecalibration();
        // If device was previously OUT_OF_SERVICE or requires recalibration, it must be CALIBRATION_PENDING
        device.completeMaintenance(requireRecalibration);
        deviceRepository.save(device);

        maintenanceRepository.saveRecord(record);

        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "tech";
        auditService.log(username, "MAINTENANCE_COMPLETED", "MaintenanceRecord", record.getId(),
                String.format("Completed %s on %s. Cost: $%s, Downtime: %.1fh. Device Status: %s",
                        record.getMaintenanceType(), device.getId(), cost.toPlainString(), downtimeHours, device.getDeviceStatus()));
    }

    public synchronized List<AbstractMaintenanceRecord> getAllRecords() {
        return maintenanceRepository.findAllRecords();
    }

    public synchronized List<MaintenanceRequest> getAllRequests() {
        return maintenanceRepository.findAllRequests();
    }

    public synchronized List<AbstractMaintenanceRecord> getRecordsByDevice(String deviceId) {
        return maintenanceRepository.findRecordsByDeviceId(deviceId);
    }
}
