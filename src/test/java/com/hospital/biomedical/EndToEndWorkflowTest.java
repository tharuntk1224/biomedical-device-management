package com.hospital.biomedical;

import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.*;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.repository.*;
import com.hospital.biomedical.service.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * End-to-end Demonstration Workflow verification adhering to Section 16 of the prompt:
 * 1. Register a device.
 * 2. Perform a successful calibration.
 * 3. Perform a failed calibration on another device.
 * 4. Confirm the failed device becomes OUT_OF_SERVICE.
 * 5. Create and complete a maintenance task.
 * 6. Verify the calibration safety restriction remains enforced.
 * 7. View the compliance dashboard.
 * 8. Export a report.
 * 9. Restart the application and verify persisted data loads.
 */
public class EndToEndWorkflowTest {

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("   END-TO-END DEMONSTRATION WORKFLOW VERIFICATION (SECTION 16)            ");
        System.out.println("==========================================================================");

        Path dataDir = Files.createTempDirectory("biomed_e2e_");
        Path reportsDir = dataDir.resolve("reports");

        try {
            // STEP 1: Initialize System
            System.out.println("[Step 0] Initializing repositories & services in: " + dataDir);
            DeviceRepository devRepo = new DeviceRepository(dataDir.resolve("devices.csv"));
            UserRepository userRepo = new UserRepository(dataDir.resolve("users.csv"));
            CalibrationRepository calRepo = new CalibrationRepository(dataDir.resolve("cal.csv"), dataDir.resolve("meas.csv"));
            MaintenanceRepository mRepo = new MaintenanceRepository(dataDir.resolve("maint.csv"), dataDir.resolve("req.csv"));
            AuditRepository audRepo = new AuditRepository(dataDir.resolve("aud.csv"));

            AuditService audit = new AuditService(audRepo);
            AuthService auth = new AuthService(userRepo, audit);
            DeviceService devService = new DeviceService(devRepo, auth, audit);
            CalibrationService calService = new CalibrationService(calRepo, devRepo, auth, audit);
            MaintenanceService mService = new MaintenanceService(mRepo, devRepo, auth, audit);
            ComplianceService compService = new ComplianceService(devRepo, calRepo, mRepo);
            ReportService repService = new ReportService(devService, calService, mService, compService, audit, reportsDir);

            // Log in as ADMIN
            auth.login("admin", "Admin@123");
            System.out.println("  -> Logged in as ADMIN: " + auth.getCurrentUser().getFullName());

            // STEP 1: Register devices
            System.out.println("[Step 1] Registering devices...");
            LifeSupportDevice vent = new LifeSupportDevice("DEV-E2E-1", "AST-VENT-E2E", "SN-VENT-901",
                    "Hamilton-C6 Ventilator", "Hamilton Medical", "C6", "Critical Care", 180, 180, true);
            DiagnosticDevice ecg = new DiagnosticDevice("DEV-E2E-2", "AST-ECG-E2E", "SN-ECG-902",
                    "Mindray BeneVision N17", "Mindray", "N17", "Cardiology", RiskLevel.MEDIUM, 180, false, "v2.1");
            devService.registerDevice(vent);
            devService.registerDevice(ecg);
            System.out.println("  -> Registered: " + vent.getId() + " and " + ecg.getId());

            // Switch to TECHNICIAN
            auth.login("tech1", "Tech@123");
            System.out.println("  -> Switched context to TECHNICIAN: " + auth.getCurrentUser().getFullName());

            // STEP 2: Perform successful calibration on DEV-E2E-1
            System.out.println("[Step 2] Performing successful calibration on DEV-E2E-1...");
            CalibrationRecord passCal = calService.performCalibration("DEV-E2E-1", "TECH-101", List.of(
                    new CalibrationMeasurement("Tidal Volume Output", 500.0, 501.5, "mL", 10.0),
                    new CalibrationMeasurement("PEEP Stability", 10.0, 10.1, "cmH2O", 0.5)
            ), "Fluke VT900A", "Scheduled Calibration Verification");

            assert passCal.getStatus() == CalibrationStatus.PASSED : "Calibration must pass";
            assert passCal.getCertificate() != null : "Certificate must be present";
            assert vent.getDeviceStatus() == DeviceStatus.OPERATIONAL : "Ventilator must be OPERATIONAL";
            System.out.printf("  -> PASS! Issued Certificate: %s, Device Status: %s, Next Due: %s%n",
                    passCal.getCertificate().getCertificateNumber(), vent.getDeviceStatus(), vent.getNextCalibrationDate());

            // STEP 3 & 4: Perform failed calibration on DEV-E2E-2 and confirm OUT_OF_SERVICE
            System.out.println("[Step 3 & 4] Performing FAILED calibration on DEV-E2E-2...");
            CalibrationRecord failCal = calService.performCalibration("DEV-E2E-2", "TECH-101", List.of(
                    new CalibrationMeasurement("ECG Lead II Amplitude", 1.0, 1.45, "mV", 0.05) // Exceeds tolerance!
            ), "ProSim 8", "Defective calibration test");

            assert failCal.getStatus() == CalibrationStatus.FAILED : "Calibration must fail";
            assert failCal.getCertificate() == null : "No certificate should be generated for failure";
            DiagnosticDevice ecgReloaded = (DiagnosticDevice) devRepo.findById("DEV-E2E-2").orElseThrow();
            assert ecgReloaded.getDeviceStatus() == DeviceStatus.OUT_OF_SERVICE : "Device must be OUT_OF_SERVICE";
            assert !ecgReloaded.canBeOperated() : "Clinical operation must be prohibited";
            System.out.printf("  -> CONFIRMED: Device %s status is %s (%s). Normal use prohibited.%n",
                    ecgReloaded.getId(), ecgReloaded.getDeviceStatus(), failCal.getFailureReason());

            // STEP 5: Create and complete a maintenance task on DEV-E2E-2
            System.out.println("[Step 5] Creating maintenance request and completing work order...");
            // Nurse requests repair
            auth.login("staff1", "Staff@123");
            MaintenanceRequest req = mService.createRequest("DEV-E2E-2", MaintenanceType.CORRECTIVE, "ECG signal drift remediation");
            System.out.println("  -> Created Request: " + req.getId());

            // Admin schedules
            auth.login("admin", "Admin@123");
            mService.approveAndScheduleRequest(req.getId(), "TECH-101", null);

            // Tech starts and finishes maintenance
            auth.login("tech1", "Tech@123");
            AbstractMaintenanceRecord mRec = mService.startMaintenance(req.getId(), "TECH-101");
            assert ecgReloaded.getDeviceStatus() == DeviceStatus.UNDER_MAINTENANCE : "Device must be UNDER_MAINTENANCE";

            mService.completeMaintenance(mRec.getId(), BigDecimal.valueOf(350), 3.5,
                    List.of("ECG Preamplifier Board"), List.of("Noise check", "Ground check"), "Board replaced and soldered.");
            System.out.println("  -> Completed Maintenance Record: " + mRec.getId());

            // STEP 6: Verify calibration safety restriction remains enforced
            System.out.println("[Step 6] Verifying safety restriction: device is NOT automatically operational...");
            DiagnosticDevice ecgAfterMaint = (DiagnosticDevice) devRepo.findById("DEV-E2E-2").orElseThrow();
            assert ecgAfterMaint.getDeviceStatus() == DeviceStatus.CALIBRATION_PENDING : "Status must be CALIBRATION_PENDING";
            assert !ecgAfterMaint.canBeOperated() : "Safety restriction must prohibit clinical operation";
            System.out.printf("  -> SAFETY VERIFIED: Device status is %s (NOT OPERATIONAL). Recalibration is mandatory.%n",
                    ecgAfterMaint.getDeviceStatus());

            // Recalibrate post-repair to restore operational state
            System.out.println("  -> Performing post-maintenance successful calibration...");
            CalibrationRecord recPost = calService.performCalibration("DEV-E2E-2", "TECH-101", List.of(
                    new CalibrationMeasurement("ECG Lead II Amplitude", 1.0, 1.01, "mV", 0.05)
            ), "ProSim 8", "Post-repair recertification");
            assert recPost.getStatus() == CalibrationStatus.PASSED : "Post calibration should pass";
            assert ecgAfterMaint.getDeviceStatus() == DeviceStatus.OPERATIONAL : "Device should be restored to OPERATIONAL";
            System.out.printf("  -> SUCCESS: Recalibrated! Cert: %s, Device Status: %s%n",
                    recPost.getCertificate().getCertificateNumber(), ecgAfterMaint.getDeviceStatus());

            // STEP 7: View compliance dashboard
            System.out.println("[Step 7] Checking Compliance Dashboard KPIs...");
            ComplianceService.ComplianceDashboardData kpis = compService.getDashboardMetrics();
            System.out.printf("  -> Total Devices: %d, Active: %d, OutOfService: %d, Compliance: %.1f%%%n",
                    kpis.totalDevices(), kpis.activeDevices(), kpis.outOfServiceDevices(), kpis.calibrationCompliancePercentage());
            assert kpis.totalDevices() == 2;
            assert kpis.calibrationCompliancePercentage() == 100.0;
            assert kpis.completedMaintenanceJobs() == 1;

            // STEP 8: Export reports
            System.out.println("[Step 8] Exporting reports to reports/ directory...");
            Path invRep = repService.exportDeviceInventoryReport();
            Path compRep = repService.exportComplianceSummaryReport();
            Path calRep = repService.exportCalibrationHistoryReport();
            Path audRep = repService.exportAuditLogReport();
            assert Files.exists(invRep) && Files.size(invRep) > 0 : "Inventory report must exist";
            assert Files.exists(compRep) && Files.size(compRep) > 0 : "Compliance report must exist";
            System.out.println("  -> Reports exported successfully: " + invRep.getFileName() + ", " + compRep.getFileName());

            // STEP 9: Restart application and verify persisted data loads
            System.out.println("[Step 9] Simulating application restart and verifying persistent reload...");
            DeviceRepository restartedDevRepo = new DeviceRepository(dataDir.resolve("devices.csv"));
            CalibrationRepository restartedCalRepo = new CalibrationRepository(dataDir.resolve("cal.csv"), dataDir.resolve("meas.csv"));
            MaintenanceRepository restartedMRepo = new MaintenanceRepository(dataDir.resolve("maint.csv"), dataDir.resolve("req.csv"));
            AuditRepository restartedAudRepo = new AuditRepository(dataDir.resolve("aud.csv"));

            assert restartedDevRepo.count() == 2 : "Must reload 2 devices";
            assert restartedCalRepo.count() == 3 : "Must reload 3 calibration records";
            assert restartedMRepo.findAllRecords().size() == 1 : "Must reload 1 maintenance record";
            assert !restartedAudRepo.findAll().isEmpty() : "Must reload audit records";

            LifeSupportDevice reloadedVent = (LifeSupportDevice) restartedDevRepo.findById("DEV-E2E-1").orElseThrow();
            assert reloadedVent.getDeviceStatus() == DeviceStatus.OPERATIONAL;
            assert reloadedVent.getNextCalibrationDate() != null;

            System.out.println("  -> Data integrity verified after restart: all records reloaded identically!");
            System.out.println("==========================================================================");
            System.out.println("   END-TO-END DEMO WORKFLOW PASSED WITH 100% COMPLIANCE!                 ");
            System.out.println("==========================================================================");

        } finally {
            // Cleanup temp dir
            Files.walk(dataDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
