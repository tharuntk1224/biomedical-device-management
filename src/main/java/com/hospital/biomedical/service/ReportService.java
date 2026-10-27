package com.hospital.biomedical.service;

import com.hospital.biomedical.model.audit.AuditLog;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.util.CsvUtils;
import com.hospital.biomedical.util.DateUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service generating and exporting structured hospital compliance, inventory, and audit reports.
 */
public class ReportService {
    private final DeviceService deviceService;
    private final CalibrationService calibrationService;
    private final MaintenanceService maintenanceService;
    private final ComplianceService complianceService;
    private final AuditService auditService;
    private final Path reportsDirectory;

    public ReportService(DeviceService deviceService, CalibrationService calibrationService,
                         MaintenanceService maintenanceService, ComplianceService complianceService,
                         AuditService auditService) {
        this(deviceService, calibrationService, maintenanceService, complianceService, auditService, Paths.get("reports"));
    }

    public ReportService(DeviceService deviceService, CalibrationService calibrationService,
                         MaintenanceService maintenanceService, ComplianceService complianceService,
                         AuditService auditService, Path reportsDirectory) {
        this.deviceService = deviceService;
        this.calibrationService = calibrationService;
        this.maintenanceService = maintenanceService;
        this.complianceService = complianceService;
        this.auditService = auditService;
        this.reportsDirectory = reportsDirectory;
        try {
            if (!Files.exists(reportsDirectory)) {
                Files.createDirectories(reportsDirectory);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create reports directory: " + e.getMessage());
        }
    }

    public Path exportDeviceInventoryReport() throws IOException {
        List<BiomedicalDevice> devices = deviceService.findDevices();
        List<String> lines = new ArrayList<>();
        if (!devices.isEmpty()) {
            lines.add(CsvUtils.toCsvLine(devices.get(0).getReportHeaders()));
            for (BiomedicalDevice d : devices) {
                lines.add(CsvUtils.toCsvLine(d.toReportRow()));
            }
        } else {
            lines.add("Device ID,Asset No,Name,Category,Department,Status,Risk Level,Calib Due,Next Calib Date");
        }
        Path target = reportsDirectory.resolve("device_inventory_report.csv");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }

    public Path exportCalibrationHistoryReport() throws IOException {
        List<CalibrationRecord> records = calibrationService.getAllCalibrations();
        List<String> lines = new ArrayList<>();
        if (!records.isEmpty()) {
            lines.add(CsvUtils.toCsvLine(records.get(0).getReportHeaders()));
            for (CalibrationRecord r : records) {
                lines.add(CsvUtils.toCsvLine(r.toReportRow()));
            }
        } else {
            lines.add("Calib ID,Device ID,Date,Status,Tech ID,Cert No,Measurements,Failure Notes");
        }
        Path target = reportsDirectory.resolve("calibration_history_report.csv");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }

    public Path exportOverdueCalibrationReport() throws IOException {
        List<BiomedicalDevice> overdueDevices = deviceService.findDevices(CalibrationDueStatus.OVERDUE);
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtils.toCsvLine("Device ID", "Asset No", "Name", "Department", "Last Calib", "Next Due", "Assigned Tech"));
        for (BiomedicalDevice d : overdueDevices) {
            lines.add(CsvUtils.toCsvLine(
                    d.getId(),
                    d.getAssetNumber(),
                    d.getName(),
                    d.getDepartment(),
                    DateUtils.formatDate(d.getLastCalibrationDate()),
                    DateUtils.formatDate(d.getNextCalibrationDate()),
                    d.getAssignedTechnicianId()
            ));
        }
        Path target = reportsDirectory.resolve("overdue_calibrations_report.csv");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }

    public Path exportMaintenanceHistoryReport() throws IOException {
        List<AbstractMaintenanceRecord> records = maintenanceService.getAllRecords();
        List<String> lines = new ArrayList<>();
        if (!records.isEmpty()) {
            lines.add(CsvUtils.toCsvLine(records.get(0).getReportHeaders()));
            for (AbstractMaintenanceRecord r : records) {
                lines.add(CsvUtils.toCsvLine(r.toReportRow()));
            }
        } else {
            lines.add("Record ID,Device ID,Type,Status,Technician,Cost,Downtime (h),Scheduled,Completed");
        }
        Path target = reportsDirectory.resolve("maintenance_history_report.csv");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }

    public Path exportComplianceSummaryReport() throws IOException {
        ComplianceService.ComplianceDashboardData data = complianceService.getDashboardMetrics();
        List<String> lines = new ArrayList<>();
        lines.add("================================================================================");
        lines.add("         HOSPITAL BIOMEDICAL ENGINEERING COMPLIANCE & SAFETY AUDIT REPORT       ");
        lines.add("================================================================================");
        lines.add("Generated At: " + DateUtils.formatDateTime(LocalDateTime.now()));
        lines.add("Hospital: Central Memorial Health System");
        lines.add("Scope: Biomedical Device Safety & Regulatory Audit Simulation");
        lines.add("--------------------------------------------------------------------------------");
        lines.add("EQUIPMENT INVENTORY METRICS:");
        lines.add("  Total Registered Devices          : " + data.totalDevices());
        lines.add("  Active Devices (In-Service)       : " + data.activeDevices());
        lines.add("  Devices Under Maintenance         : " + data.underMaintenanceDevices());
        lines.add("  Devices Out of Service (Locked)   : " + data.outOfServiceDevices());
        lines.add("");
        lines.add("CALIBRATION COMPLIANCE STATUS:");
        lines.add("  Valid Calibrations                : " + data.validCalibrations());
        lines.add("  Due Soon (Next 7 Days)            : " + data.dueSoonCalibrations());
        lines.add("  Due Today                         : " + data.dueTodayCalibrations());
        lines.add("  Overdue Calibrations              : " + data.overdueCalibrations());
        lines.add("  Calibration Exempt Devices        : " + data.notRequiredCalibrations());
        lines.add("  Failed Calibration Events         : " + data.failedCalibrations());
        lines.add("");
        lines.add("KEY PERFORMANCE INDICATOR (KPI):");
        lines.add(String.format("  Overall Calibration Compliance    : %.1f%% (%d of %d devices compliant)",
                data.calibrationCompliancePercentage(), data.compliantDevicesCount(), data.subjectDevicesCount()));
        lines.add("  Formula: (Valid + Due Soon) / (Total Requiring Periodic Calibration) * 100");
        lines.add("");
        lines.add("MAINTENANCE OPERATIONS & DOWNTIME:");
        lines.add("  Active Open Work Orders           : " + data.openMaintenanceRequests());
        lines.add("  Completed Maintenance Jobs        : " + data.completedMaintenanceJobs());
        lines.add("  Total Maintenance Expenditure     : $" + data.totalMaintenanceCost().toPlainString());
        lines.add(String.format("  Total Cumulative Downtime         : %.1f Hours", data.totalDowntimeHours()));
        lines.add("================================================================================");
        lines.add("IMPORTANT SAFETY DISCLAIMER:");
        lines.add("This simulated compliance report is generated for educational and training");
        lines.add("purposes only. It does not certify clinical adherence to ISO 17025 or FDA 21 CFR 820.");
        lines.add("================================================================================");

        Path target = reportsDirectory.resolve("compliance_summary_report.txt");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }

    public Path exportAuditLogReport() throws IOException {
        List<AuditLog> logs = auditService.getAllLogs();
        List<String> lines = new ArrayList<>();
        if (!logs.isEmpty()) {
            lines.add(CsvUtils.toCsvLine(logs.get(0).getReportHeaders()));
            for (AuditLog l : logs) {
                lines.add(CsvUtils.toCsvLine(l.toReportRow()));
            }
        } else {
            lines.add("Event ID,Timestamp,User,Action,Target,Target ID,Details");
        }
        Path target = reportsDirectory.resolve("audit_logs_report.csv");
        CsvUtils.writeLinesAtomic(target, lines);
        return target;
    }
}
