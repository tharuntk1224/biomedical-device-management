package com.hospital.biomedical.service;

import com.hospital.biomedical.exception.DeviceNotFoundException;
import com.hospital.biomedical.exception.DuplicateEntityException;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.UserRole;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.util.IdGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing Biomedical Device lifecycle, registration, queries, and retirement.
 * Enforces role-based permissions and demonstrates Polymorphism through overloaded search methods.
 */
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public DeviceService(DeviceRepository deviceRepository, AuthService authService, AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    public synchronized BiomedicalDevice registerDevice(BiomedicalDevice device) {
        authService.checkPermission(UserRole.ADMIN);
        Objects.requireNonNull(device, "Device cannot be null.");

        if (device.getId() == null || device.getId().trim().isEmpty()) {
            device.setId(IdGenerator.nextDeviceId());
        }

        if (deviceRepository.existsById(device.getId())) {
            throw new DuplicateEntityException("Device with ID '" + device.getId() + "' already exists.");
        }
        if (deviceRepository.existsByAssetNumber(device.getAssetNumber())) {
            throw new DuplicateEntityException("Device with Asset Number '" + device.getAssetNumber() + "' already exists.");
        }

        deviceRepository.save(device);
        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "SYSTEM";
        auditService.log(username, "DEVICE_REGISTERED", "BiomedicalDevice", device.getId(),
                "Registered " + device.getName() + " (" + device.getAssetNumber() + ") in " + device.getDepartment());
        return device;
    }

    public synchronized BiomedicalDevice getDeviceById(String id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Biomedical device not found with ID: " + id));
    }

    public synchronized Optional<BiomedicalDevice> findByAssetNumber(String assetNumber) {
        return deviceRepository.findByAssetNumber(assetNumber);
    }

    // Overloaded search methods (Polymorphism)
    public synchronized List<BiomedicalDevice> findDevices() {
        return deviceRepository.findDevices();
    }

    public synchronized List<BiomedicalDevice> findDevices(String department) {
        return deviceRepository.findDevices(department);
    }

    public synchronized List<BiomedicalDevice> findDevices(CalibrationDueStatus dueStatus) {
        return deviceRepository.findDevices(dueStatus, LocalDate.now());
    }

    public synchronized List<BiomedicalDevice> findDevices(DeviceStatus status) {
        return deviceRepository.findDevices(status);
    }

    public synchronized BiomedicalDevice updateDevice(BiomedicalDevice updated) {
        authService.checkPermission(UserRole.ADMIN);
        Objects.requireNonNull(updated, "Device cannot be null.");

        BiomedicalDevice existing = getDeviceById(updated.getId());
        if (existing.getDeviceStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("Retired devices cannot be updated.");
        }

        existing.setName(updated.getName());
        existing.setManufacturer(updated.getManufacturer());
        existing.setModelNumber(updated.getModelNumber());
        existing.setDepartment(updated.getDepartment());
        existing.setRiskLevel(updated.getRiskLevel());
        existing.setCalibrationRequired(updated.isCalibrationRequired());
        existing.setCalibrationFrequencyDays(updated.getCalibrationFrequencyDays());
        existing.setAssignedTechnicianId(updated.getAssignedTechnicianId());

        deviceRepository.save(existing);
        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "SYSTEM";
        auditService.log(username, "DEVICE_UPDATED", "BiomedicalDevice", existing.getId(), "Updated metadata for " + existing.getName());
        return existing;
    }

    public synchronized BiomedicalDevice retireDevice(String deviceId, String reason) {
        authService.checkPermission(UserRole.ADMIN);
        BiomedicalDevice device = getDeviceById(deviceId);

        if (device.getDeviceStatus() == DeviceStatus.RETIRED) {
            return device; // Already retired
        }

        device.setDeviceStatus(DeviceStatus.RETIRED);
        deviceRepository.save(device);

        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "SYSTEM";
        auditService.log(username, "DEVICE_RETIRED", "BiomedicalDevice", device.getId(),
                "Decommissioned permanently. Reason: " + (reason != null ? reason : "End of life"));
        return device;
    }

    public synchronized void assignTechnician(String deviceId, String technicianId) {
        authService.checkPermission(UserRole.ADMIN);
        BiomedicalDevice device = getDeviceById(deviceId);
        device.setAssignedTechnicianId(technicianId);
        deviceRepository.save(device);

        String username = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "SYSTEM";
        auditService.log(username, "TECHNICIAN_ASSIGNED", "BiomedicalDevice", device.getId(),
                "Assigned technician " + technicianId + " to device " + device.getName());
    }
}
