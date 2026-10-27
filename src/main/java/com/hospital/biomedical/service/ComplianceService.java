package com.hospital.biomedical.service;

import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.CalibrationStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.repository.CalibrationRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.MaintenanceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service aggregating hospital equipment safety metrics, maintenance costs,
 * downtime, and calibration compliance KPIs.
 */
public class ComplianceService {
    private final DeviceRepository deviceRepository;
    private final CalibrationRepository calibrationRepository;
    private final MaintenanceRepository maintenanceRepository;

    public ComplianceService(DeviceRepository deviceRepository,
                             CalibrationRepository calibrationRepository,
                             MaintenanceRepository maintenanceRepository) {
        this.deviceRepository = deviceRepository;
        this.calibrationRepository = calibrationRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public ComplianceDashboardData getDashboardMetrics() {
        return getDashboardMetrics(LocalDate.now());
    }

    public ComplianceDashboardData getDashboardMetrics(LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        List<BiomedicalDevice> devices = deviceRepository.findDevices();
        List<CalibrationRecord> calibrations = calibrationRepository.findAll();
        List<AbstractMaintenanceRecord> maintenanceRecords = maintenanceRepository.findAllRecords();
        List<MaintenanceRequest> maintenanceRequests = maintenanceRepository.findAllRequests();

        int totalDevices = devices.size();
        int activeDevices = 0;
        int outOfServiceDevices = 0;
        int underMaintenanceDevices = 0;

        int validCalibrations = 0;
        int dueSoonCalibrations = 0;
        int dueTodayCalibrations = 0;
        int overdueCalibrations = 0;
        int notRequiredCalibrations = 0;

        int calibSubjectCount = 0;
        int calibCompliantCount = 0;

        for (BiomedicalDevice d : devices) {
            if (d.getDeviceStatus() == DeviceStatus.OPERATIONAL) {
                activeDevices++;
            } else if (d.getDeviceStatus() == DeviceStatus.OUT_OF_SERVICE) {
                outOfServiceDevices++;
            } else if (d.getDeviceStatus() == DeviceStatus.UNDER_MAINTENANCE) {
                underMaintenanceDevices++;
            }

            CalibrationDueStatus dueStatus = d.getCalibrationDueStatus(refDate);
            switch (dueStatus) {
                case VALID -> validCalibrations++;
                case DUE_SOON -> dueSoonCalibrations++;
                case DUE_TODAY -> dueTodayCalibrations++;
                case OVERDUE -> overdueCalibrations++;
                case NOT_REQUIRED -> notRequiredCalibrations++;
            }

            if (d.isCalibrationRequired()) {
                calibSubjectCount++;
                if (dueStatus == CalibrationDueStatus.VALID || dueStatus == CalibrationDueStatus.DUE_SOON) {
                    calibCompliantCount++;
                }
            }
        }

        // Compliance % = (Valid + DueSoon) / Total Requiring Calibration * 100
        double compliancePercentage = calibSubjectCount > 0
                ? ((double) calibCompliantCount / calibSubjectCount) * 100.0
                : 100.0;
        compliancePercentage = Math.round(compliancePercentage * 10.0) / 10.0;

        // Calibrations count
        long failedCalibrations = calibrations.stream()
                .filter(c -> c.getStatus() == CalibrationStatus.FAILED)
                .count();

        // Maintenance metrics
        long openRequests = maintenanceRequests.stream()
                .filter(r -> r.getStatus() != MaintenanceStatus.COMPLETED && r.getStatus() != MaintenanceStatus.CANCELLED)
                .count();

        long completedJobs = maintenanceRecords.stream()
                .filter(m -> m.getMaintenanceStatus() == MaintenanceStatus.COMPLETED)
                .count();

        BigDecimal totalCost = BigDecimal.ZERO;
        double totalDowntime = 0.0;

        for (AbstractMaintenanceRecord m : maintenanceRecords) {
            if (m.getCost() != null) {
                totalCost = totalCost.add(m.getCost());
            }
            totalDowntime += m.getDowntimeHours();
        }

        return new ComplianceDashboardData(
                totalDevices,
                activeDevices,
                outOfServiceDevices,
                underMaintenanceDevices,
                validCalibrations,
                dueSoonCalibrations,
                dueTodayCalibrations,
                overdueCalibrations,
                notRequiredCalibrations,
                (int) failedCalibrations,
                (int) openRequests,
                (int) completedJobs,
                totalCost,
                totalDowntime,
                compliancePercentage,
                calibCompliantCount,
                calibSubjectCount
        );
    }

    /**
     * Immutable value object holding consolidated dashboard KPIs.
     */
    public record ComplianceDashboardData(
            int totalDevices,
            int activeDevices,
            int outOfServiceDevices,
            int underMaintenanceDevices,
            int validCalibrations,
            int dueSoonCalibrations,
            int dueTodayCalibrations,
            int overdueCalibrations,
            int notRequiredCalibrations,
            int failedCalibrations,
            int openMaintenanceRequests,
            int completedMaintenanceJobs,
            BigDecimal totalMaintenanceCost,
            double totalDowntimeHours,
            double calibrationCompliancePercentage,
            int compliantDevicesCount,
            int subjectDevicesCount
    ) {}
}
