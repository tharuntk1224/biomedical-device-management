package com.hospital.biomedical;

import com.hospital.biomedical.exception.DuplicateEntityException;
import com.hospital.biomedical.exception.InvalidStateTransitionException;
import com.hospital.biomedical.exception.UnauthorizedAccessException;
import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LaboratoryDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.*;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.model.maintenance.PreventiveMaintenance;
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
 * Standalone Test Suite Runner for self-contained automated test verification.
 * Runs offline with zero external dependencies and executes all core test scenarios.
 */
public class StandaloneTestRunner {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   BIOMEDICAL SYSTEM - AUTOMATED TEST SUITE VERIFICATION                  ");
        System.out.println("==========================================================================");

        runTest("Measurement Within Tolerance Passes", StandaloneTestRunner::testMeasurementWithinTolerance);
        runTest("Measurement Exceeding Tolerance Fails", StandaloneTestRunner::testMeasurementOutsideTolerance);
        runTest("Calibration with No Measurements Fails", StandaloneTestRunner::testNoMeasurementCalibrationFails);
        runTest("Successful Calibration Sets Due Date & Certificate", StandaloneTestRunner::testSuccessfulCalibrationWorkflow);
        runTest("Failed Calibration Marks Device OUT_OF_SERVICE", StandaloneTestRunner::testFailedCalibrationLockout);
        runTest("Calibration Due-Status Boundaries", StandaloneTestRunner::testDueStatusBoundaries);
        runTest("Device ID & Asset Number Uniqueness", StandaloneTestRunner::testDeviceUniqueness);
        runTest("Negative Maintenance Cost & Downtime Rejection", StandaloneTestRunner::testNegativeCostAndDowntime);
        runTest("Invalid Maintenance State Transitions Rejected", StandaloneTestRunner::testInvalidMaintenanceTransitions);
        runTest("Post-Maintenance Safety Recalibration Enforced", StandaloneTestRunner::testPostMaintenanceSafety);
        runTest("Compliance KPI Calculation & Exempt Device Handling", StandaloneTestRunner::testComplianceKpiCalculation);
        runTest("Role-Based Access Restrictions Enforced", StandaloneTestRunner::testRolePermissions);
        runTest("Audit Logging Verification", StandaloneTestRunner::testAuditLogging);
        runTest("Persistence & Reload Across Application Restarts", StandaloneTestRunner::testPersistenceAndReload);
        runTest("BioMed AI Copilot Advisory & Grounding Verification", StandaloneTestRunner::testAiAssistantService);

        System.out.println("==========================================================================");
        System.out.printf("   TEST RESULTS: %d PASSED, %d FAILED (TOTAL: %d)%n",
                testsPassed, testsFailed, (testsPassed + testsFailed));
        System.out.println("==========================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase test) {
        try {
            test.execute();
            System.out.printf("  [PASS] %-55s%n", testName);
            testsPassed++;
        } catch (Throwable t) {
            System.out.printf("  [FAIL] %-55s -> %s%n", testName, t.getMessage());
            testsFailed++;
        }
    }

    @FunctionalInterface
    interface TestCase {
        void execute() throws Exception;
    }

    private static Path createTempDir() throws IOException {
        return Files.createTempDirectory("biomed_test_");
    }

    private static void deleteDir(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }

    // 1. Measurement within tolerance
    static void testMeasurementWithinTolerance() {
        CalibrationMeasurement m = new CalibrationMeasurement("Tidal Volume", 500.0, 502.5, "mL", 5.0);
        if (m.getDeviation() != 2.5) throw new AssertionError("Expected deviation 2.5, got: " + m.getDeviation());
        if (!m.isPassed()) throw new AssertionError("Measurement within tolerance should pass.");
    }

    // 2. Measurement exceeding tolerance
    static void testMeasurementOutsideTolerance() {
        CalibrationMeasurement m = new CalibrationMeasurement("Pressure", 10.0, 12.5, "cmH2O", 1.0);
        if (m.getDeviation() != 2.5) throw new AssertionError("Expected deviation 2.5, got: " + m.getDeviation());
        if (m.isPassed()) throw new AssertionError("Measurement exceeding tolerance should fail.");
    }

    // 3. Calibration with no measurements fails
    static void testNoMeasurementCalibrationFails() {
        CalibrationRecord record = new CalibrationRecord("CAL-T1", "DEV-1001", "TECH-101", LocalDate.now(), "Tool", "Notes");
        boolean passed = record.evaluate();
        if (passed) throw new AssertionError("Evaluation with 0 measurements must fail.");
        if (record.getStatus() != CalibrationStatus.FAILED) throw new AssertionError("Status must be FAILED.");
        if (record.getCertificate() != null) throw new AssertionError("Certificate must not be issued.");
    }

    // 4. Successful calibration sets due date and certificate
    static void testSuccessfulCalibrationWorkflow() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            CalibrationRepository calRepo = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("meas.csv"));
            UserRepository userRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(userRepo, audit);
            CalibrationService calService = new CalibrationService(calRepo, devRepo, auth, audit);

            auth.login("tech1", "Tech@123");

            LifeSupportDevice vent = new LifeSupportDevice("DEV-T1", "AST-T1", "SN-T1", "Ventilator", "OEM", "M1", "ICU", 180, 120, true);
            devRepo.save(vent);

            LocalDate testDate = LocalDate.of(2026, 4, 1);
            CalibrationRecord rec = calService.performCalibration("DEV-T1", "TECH-101", List.of(
                    new CalibrationMeasurement("PEEP", 10.0, 10.1, "cmH2O", 0.5)
            ), "Fluke VT900A", "Annual PM", testDate);

            if (rec.getStatus() != CalibrationStatus.PASSED) throw new AssertionError("Calibration should pass.");
            if (rec.getCertificate() == null) throw new AssertionError("Certificate should be generated.");

            LifeSupportDevice updated = (LifeSupportDevice) devRepo.findById("DEV-T1").orElseThrow();
            if (updated.getDeviceStatus() != DeviceStatus.OPERATIONAL) throw new AssertionError("Status should be OPERATIONAL.");
            if (!testDate.plusDays(180).equals(updated.getNextCalibrationDate())) {
                throw new AssertionError("Next due date calculation incorrect.");
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    // 5. Failed calibration lockout
    static void testFailedCalibrationLockout() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            CalibrationRepository calRepo = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("meas.csv"));
            UserRepository userRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(userRepo, audit);
            CalibrationService calService = new CalibrationService(calRepo, devRepo, auth, audit);

            auth.login("tech1", "Tech@123");

            LifeSupportDevice defib = new LifeSupportDevice("DEV-T2", "AST-T2", "SN-T2", "Defib", "OEM", "M2", "ER", 90, 120, true);
            devRepo.save(defib);

            CalibrationRecord rec = calService.performCalibration("DEV-T2", "TECH-101", List.of(
                    new CalibrationMeasurement("Energy (200J)", 200.0, 150.0, "Joules", 5.0) // Fails!
            ), "Impulse 7000", "Failed Test", LocalDate.now());

            if (rec.getStatus() != CalibrationStatus.FAILED) throw new AssertionError("Status should be FAILED.");
            if (rec.getCertificate() != null) throw new AssertionError("Certificate must not exist on failure.");

            LifeSupportDevice updated = (LifeSupportDevice) devRepo.findById("DEV-T2").orElseThrow();
            if (updated.getDeviceStatus() != DeviceStatus.OUT_OF_SERVICE) {
                throw new AssertionError("Device must be OUT_OF_SERVICE after failed calibration.");
            }
            if (updated.canBeOperated()) throw new AssertionError("Device cannot be operated.");
        } finally {
            deleteDir(tempDir);
        }
    }

    // 6. Due-status boundaries
    static void testDueStatusBoundaries() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        LifeSupportDevice d = new LifeSupportDevice("D", "A", "S", "N", "M", "MOD", "ICU", 180, 120, true);

        d.setCalibrationRequired(false);
        if (d.getCalibrationDueStatus(today) != CalibrationDueStatus.NOT_REQUIRED) throw new AssertionError();
        d.setCalibrationRequired(true);

        d.setNextCalibrationDate(today.minusDays(1));
        if (d.getCalibrationDueStatus(today) != CalibrationDueStatus.OVERDUE) throw new AssertionError();

        d.setNextCalibrationDate(today);
        if (d.getCalibrationDueStatus(today) != CalibrationDueStatus.DUE_TODAY) throw new AssertionError();

        d.setNextCalibrationDate(today.plusDays(5));
        if (d.getCalibrationDueStatus(today) != CalibrationDueStatus.DUE_SOON) throw new AssertionError();

        d.setNextCalibrationDate(today.plusDays(15));
        if (d.getCalibrationDueStatus(today) != CalibrationDueStatus.VALID) throw new AssertionError();
    }

    // 7. Device ID & Asset uniqueness
    static void testDeviceUniqueness() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository repo = new DeviceRepository(tempDir.resolve("dev.csv"));
            UserRepository uRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(uRepo, audit);
            DeviceService service = new DeviceService(repo, auth, audit);

            auth.login("admin", "Admin@123");

            LifeSupportDevice d1 = new LifeSupportDevice("DEV-U1", "AST-U1", "SN1", "Name 1", "OEM", "M1", "ICU", 180, 120, true);
            service.registerDevice(d1);

            boolean duplicateIdCaught = false;
            try {
                LifeSupportDevice d2 = new LifeSupportDevice("DEV-U1", "AST-U2", "SN2", "Name 2", "OEM", "M1", "ICU", 180, 120, true);
                service.registerDevice(d2);
            } catch (DuplicateEntityException e) {
                duplicateIdCaught = true;
            }
            if (!duplicateIdCaught) throw new AssertionError("Duplicate ID was not caught.");

            boolean duplicateAssetCaught = false;
            try {
                LifeSupportDevice d3 = new LifeSupportDevice("DEV-U3", "AST-U1", "SN3", "Name 3", "OEM", "M1", "ICU", 180, 120, true);
                service.registerDevice(d3);
            } catch (DuplicateEntityException e) {
                duplicateAssetCaught = true;
            }
            if (!duplicateAssetCaught) throw new AssertionError("Duplicate Asset Number was not caught.");
        } finally {
            deleteDir(tempDir);
        }
    }

    // 8. Negative cost and downtime rejection
    static void testNegativeCostAndDowntime() {
        PreventiveMaintenance pm = new PreventiveMaintenance();
        boolean costCaught = false;
        try {
            pm.setCost(new BigDecimal("-50.00"));
        } catch (IllegalArgumentException e) {
            costCaught = true;
        }
        if (!costCaught) throw new AssertionError("Negative cost should throw IllegalArgumentException.");

        boolean downtimeCaught = false;
        try {
            pm.setDowntimeHours(-2.0);
        } catch (IllegalArgumentException e) {
            downtimeCaught = true;
        }
        if (!downtimeCaught) throw new AssertionError("Negative downtime should throw IllegalArgumentException.");
    }

    // 9. Invalid maintenance transitions
    static void testInvalidMaintenanceTransitions() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            MaintenanceRepository mRepo = new MaintenanceRepository(tempDir.resolve("rec.csv"), tempDir.resolve("req.csv"));
            UserRepository uRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(uRepo, audit);
            MaintenanceService service = new MaintenanceService(mRepo, devRepo, auth, audit);

            auth.login("admin", "Admin@123");
            LifeSupportDevice vent = new LifeSupportDevice("DEV-M1", "AST-M1", "SN-M1", "Ventilator", "OEM", "V1", "ICU", 180, 120, true);
            devRepo.save(vent);

            MaintenanceRequest req = service.createRequest("DEV-M1", MaintenanceType.PREVENTIVE, "Routine service");
            AbstractMaintenanceRecord rec = service.startMaintenance(req.getId(), "TECH-101");

            // Complete maintenance
            service.completeMaintenance(rec.getId(), BigDecimal.valueOf(150), 2.0, null, null, "Done");

            // Try to reopen completed maintenance
            boolean reopenCaught = false;
            try {
                service.completeMaintenance(rec.getId(), BigDecimal.valueOf(200), 3.0, null, null, "Done again");
            } catch (InvalidStateTransitionException e) {
                reopenCaught = true;
            }
            if (!reopenCaught) throw new AssertionError("Reopening completed maintenance must fail.");
        } finally {
            deleteDir(tempDir);
        }
    }

    // 10. Post-maintenance safety recalibration
    static void testPostMaintenanceSafety() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            MaintenanceRepository mRepo = new MaintenanceRepository(tempDir.resolve("rec.csv"), tempDir.resolve("req.csv"));
            UserRepository uRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(uRepo, audit);
            MaintenanceService service = new MaintenanceService(mRepo, devRepo, auth, audit);

            auth.login("admin", "Admin@123");
            LifeSupportDevice vent = new LifeSupportDevice("DEV-MS", "AST-MS", "SN-MS", "Ventilator", "OEM", "V1", "ICU", 180, 120, true);
            vent.setDeviceStatus(DeviceStatus.OUT_OF_SERVICE);
            devRepo.save(vent);

            MaintenanceRequest req = service.createRequest("DEV-MS", MaintenanceType.CORRECTIVE, "Flow sensor fix");
            AbstractMaintenanceRecord rec = service.startMaintenance(req.getId(), "TECH-101");
            service.completeMaintenance(rec.getId(), BigDecimal.valueOf(300), 3.0, List.of("Flow Sensor"), null, "Replaced");

            LifeSupportDevice updated = (LifeSupportDevice) devRepo.findById("DEV-MS").orElseThrow();
            if (updated.getDeviceStatus() != DeviceStatus.CALIBRATION_PENDING) {
                throw new AssertionError("Device must remain CALIBRATION_PENDING, not operational!");
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    // 11. Compliance KPI calculation
    static void testComplianceKpiCalculation() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            CalibrationRepository cRepo = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("m.csv"));
            MaintenanceRepository mRepo = new MaintenanceRepository(tempDir.resolve("m.csv"), tempDir.resolve("r.csv"));
            ComplianceService compService = new ComplianceService(devRepo, cRepo, mRepo);

            LocalDate today = LocalDate.of(2026, 5, 1);

            LifeSupportDevice d1 = new LifeSupportDevice("D1", "A1", "S1", "Dev 1", "OEM", "M1", "ICU", 180, 120, true);
            d1.setNextCalibrationDate(today.plusDays(30)); // Compliant (Valid)
            devRepo.save(d1);

            DiagnosticDevice d2 = new DiagnosticDevice("D2", "A2", "S2", "Dev 2", "OEM", "M2", "Rad", RiskLevel.MEDIUM, 180, false, "v1");
            d2.setNextCalibrationDate(today.plusDays(5)); // Compliant (Due Soon)
            devRepo.save(d2);

            LifeSupportDevice d3 = new LifeSupportDevice("D3", "A3", "S3", "Dev 3", "OEM", "M3", "ICU", 180, 120, true);
            d3.setNextCalibrationDate(today.minusDays(2)); // Non-compliant (Overdue)
            devRepo.save(d3);

            LaboratoryDevice d4 = new LaboratoryDevice("D4", "A4", "S4", "Dev 4", "OEM", "M4", "Lab", RiskLevel.LOW, 180, false, 22.0, 1);
            d4.setCalibrationRequired(false); // Exempt
            devRepo.save(d4);

            ComplianceService.ComplianceDashboardData data = compService.getDashboardMetrics(today);
            if (data.subjectDevicesCount() != 3) throw new AssertionError("Subject count should be 3.");
            if (data.compliantDevicesCount() != 2) throw new AssertionError("Compliant count should be 2.");
            if (Math.abs(data.calibrationCompliancePercentage() - 66.7) > 0.2) {
                throw new AssertionError("Compliance should be ~66.7%, got: " + data.calibrationCompliancePercentage());
            }
        } finally {
            deleteDir(tempDir);
        }
    }

    // 12. Role permissions
    static void testRolePermissions() throws IOException {
        Path tempDir = createTempDir();
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            UserRepository uRepo = new UserRepository(tempDir.resolve("usr.csv"));
            AuditService audit = new AuditService(new AuditRepository(tempDir.resolve("aud.csv")));
            AuthService auth = new AuthService(uRepo, audit);
            DeviceService service = new DeviceService(devRepo, auth, audit);

            auth.login("staff1", "Staff@123");
            boolean rejected = false;
            try {
                service.registerDevice(new LifeSupportDevice("D", "A", "S", "N", "O", "M", "I", 180, 120, true));
            } catch (UnauthorizedAccessException e) {
                rejected = true;
            }
            if (!rejected) throw new AssertionError("Staff should not be authorized to register devices.");
        } finally {
            deleteDir(tempDir);
        }
    }

    // 13. Audit logging
    static void testAuditLogging() throws IOException {
        Path tempDir = createTempDir();
        try {
            AuditRepository audRepo = new AuditRepository(tempDir.resolve("aud.csv"));
            AuditService audit = new AuditService(audRepo);
            audit.log("admin", "DEVICE_REGISTERED", "BiomedicalDevice", "DEV-999", "Test audit event");
            if (audRepo.findAll().isEmpty()) throw new AssertionError("Audit log was not saved.");
            if (!"DEV-999".equals(audRepo.findAll().get(0).getTargetId())) throw new AssertionError("Target ID mismatch.");
        } finally {
            deleteDir(tempDir);
        }
    }

    // 14. Persistence & reload
    static void testPersistenceAndReload() throws IOException {
        Path tempDir = createTempDir();
        try {
            Path devPath = tempDir.resolve("devices.csv");
            DeviceRepository repo1 = new DeviceRepository(devPath);

            LifeSupportDevice vent = new LifeSupportDevice("DEV-P100", "AST-P100", "SN-P100",
                    "Servo-i Ventilator", "Maquet", "Servo-i", "ICU", 180, 180, true);
            vent.setDeviceStatus(DeviceStatus.OPERATIONAL);
            repo1.save(vent);

            // Re-instantiate repository from disk
            DeviceRepository repo2 = new DeviceRepository(devPath);
            if (repo2.count() != 1) throw new AssertionError("Reloaded device count should be 1.");

            LifeSupportDevice reloaded = (LifeSupportDevice) repo2.findById("DEV-P100").orElseThrow();
            if (!"Servo-i Ventilator".equals(reloaded.getName())) throw new AssertionError("Device name mismatch after reload.");
            if (reloaded.getBatteryBackupMinutes() != 180) throw new AssertionError("Battery backup minutes mismatch.");
        } finally {
            deleteDir(tempDir);
        }
    }

    private static void testAiAssistantService() throws Exception {
        Path tempDir = Files.createTempDirectory("biomed_test_ai_");
        try {
            DeviceRepository devRepo = new DeviceRepository(tempDir.resolve("dev.csv"));
            CalibrationRepository calRepo = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("meas.csv"));
            MaintenanceRepository maintRepo = new MaintenanceRepository(tempDir.resolve("maint.csv"), tempDir.resolve("req.csv"));
            AuditRepository audRepo = new AuditRepository(tempDir.resolve("aud.csv"));
            UserRepository userRepo = new UserRepository(tempDir.resolve("usr.csv"));

            AuditService audSvc = new AuditService(audRepo);
            AuthService authSvc = new AuthService(userRepo, audSvc);
            DeviceService devSvc = new DeviceService(devRepo, authSvc, audSvc);
            CalibrationService calSvc = new CalibrationService(calRepo, devRepo, authSvc, audSvc);
            MaintenanceService maintSvc = new MaintenanceService(maintRepo, devRepo, authSvc, audSvc);
            ComplianceService compSvc = new ComplianceService(devRepo, calRepo, maintRepo);

            BiomedicalAiAssistantService aiSvc = new BiomedicalAiAssistantService(devSvc, compSvc, calSvc, maintSvc);

            // Test tolerance math question
            var resp1 = aiSvc.askAi("How is calibration deviation calculated?", null);
            if (resp1 == null || !resp1.reply().contains("Deviation")) {
                throw new AssertionError("AI assistant failed to explain calibration deviation math.");
            }

            // Test safety interlock question
            var resp2 = aiSvc.askAi("Explain the post-maintenance safety interlock rule", null);
            if (resp2 == null || !resp2.reply().contains("CALIBRATION_PENDING")) {
                throw new AssertionError("AI assistant failed to explain post-maintenance safety rule.");
            }

            // Test compliance telemetry
            var resp3 = aiSvc.askAi("What is our hospital compliance rate?", null);
            if (resp3 == null || !resp3.reply().contains("Compliance Rate")) {
                throw new AssertionError("AI assistant failed to calculate compliance telemetry.");
            }
        } finally {
            deleteDir(tempDir);
        }
    }
}
