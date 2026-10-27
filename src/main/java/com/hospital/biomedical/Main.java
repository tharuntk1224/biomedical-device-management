package com.hospital.biomedical;

import com.hospital.biomedical.exception.*;
import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.*;
import com.hospital.biomedical.model.enums.*;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.model.user.Technician;
import com.hospital.biomedical.model.user.User;
import com.hospital.biomedical.repository.*;
import com.hospital.biomedical.service.*;
import com.hospital.biomedical.util.DateUtils;
import com.hospital.biomedical.util.TablePrinter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Main application entry point for Biomedical Device Calibration & Maintenance Management System.
 * Orchestrates console interaction, service calls, and user authentication.
 */
public class Main {
    private final DeviceService deviceService;
    private final CalibrationService calibrationService;
    private final MaintenanceService maintenanceService;
    private final ComplianceService complianceService;
    private final AuthService authService;
    private final AuditService auditService;
    private final ReportService reportService;
    private final BiomedicalAiAssistantService aiAssistantService;
    private final UserRepository userRepository;
    private final Scanner scanner;

    public Main() {
        this.scanner = new Scanner(System.in);

        // Initialize repositories
        DeviceRepository deviceRepository = new DeviceRepository();
        this.userRepository = new UserRepository();
        CalibrationRepository calibrationRepository = new CalibrationRepository();
        MaintenanceRepository maintenanceRepository = new MaintenanceRepository();
        AuditRepository auditRepository = new AuditRepository();

        // Initialize services
        this.auditService = new AuditService(auditRepository);
        this.authService = new AuthService(userRepository, auditService);
        this.deviceService = new DeviceService(deviceRepository, authService, auditService);
        this.calibrationService = new CalibrationService(calibrationRepository, deviceRepository, authService, auditService);
        this.maintenanceService = new MaintenanceService(maintenanceRepository, deviceRepository, authService, auditService);
        this.complianceService = new ComplianceService(deviceRepository, calibrationRepository, maintenanceRepository);
        this.reportService = new ReportService(deviceService, calibrationService, maintenanceService, complianceService, auditService);
        this.aiAssistantService = new BiomedicalAiAssistantService(deviceService, complianceService, calibrationService, maintenanceService);

        // Seed demonstration data if initial run
        DataSeeder seeder = new DataSeeder(deviceRepository, userRepository, calibrationService, maintenanceService, authService);
        seeder.seedIfEmpty();

        // Start embedded HTTP Web Server in background daemon
        try {
            com.hospital.biomedical.web.WebServer webServer = new com.hospital.biomedical.web.WebServer(
                    deviceService, calibrationService, maintenanceService, complianceService, authService, auditService, reportService);
            int port = webServer.start();
            System.out.println("\n[HTTP SERVER] Web UI is active: http://localhost:" + port + "/");
        } catch (Exception e) {
            // Ignore if port already running
        }
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        printBanner();
        loginFlow();

        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = promptInt("Select an option (0-19): ", -1);

                switch (choice) {
                    case 1 -> handleRegisterDevice();
                    case 2 -> handleViewAllDevices();
                    case 3 -> handleSearchDevice();
                    case 4 -> handleUpdateDevice();
                    case 5 -> handleRetireDevice();
                    case 6 -> handleViewCalibrationStatus();
                    case 7 -> handlePerformCalibration();
                    case 8 -> handleViewCalibrationHistory();
                    case 9 -> handleCreateMaintenanceRequest();
                    case 10 -> handleApproveScheduleMaintenance();
                    case 11 -> handleStartMaintenance();
                    case 12 -> handleCompleteMaintenance();
                    case 13 -> handleAssignTechnician();
                    case 14 -> handleComplianceDashboard();
                    case 15 -> handleGenerateReports();
                    case 16 -> handleViewAuditLogs();
                    case 17 -> handleManageUsers();
                    case 18 -> handleSwitchUser(); // Convenience demo utility
                    case 19 -> handleAiAssistant();
                    case 0 -> {
                        System.out.println("\n[SYSTEM] Thank you for using the Biomedical Calibration Management System. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("[!] Invalid choice. Please enter a number between 0 and 19.");
                }
            } catch (UnauthorizedAccessException e) {
                System.out.println("\n[ACCESS DENIED] " + e.getMessage());
            } catch (DeviceOutOfServiceException e) {
                System.out.println("\n[SAFETY LOCKOUT] " + e.getMessage());
            } catch (InvalidStateTransitionException e) {
                System.out.println("\n[INVALID TRANSITION] " + e.getMessage());
            } catch (DeviceNotFoundException e) {
                System.out.println("\n[NOT FOUND] " + e.getMessage());
            } catch (DuplicateEntityException e) {
                System.out.println("\n[DUPLICATE ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[ERROR] An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // AUTHENTICATION FLOW
    // ==========================================
    private void loginFlow() {
        System.out.println("\n========================================================");
        System.out.println("                 HOSPITAL SYSTEM LOGIN                  ");
        System.out.println("========================================================");
        System.out.println("Demo Accounts Available:");
        System.out.println("  1) admin    / Admin@123   (ADMIN - Full Access)");
        System.out.println("  2) tech1    / Tech@123    (TECHNICIAN - Calibrations & Repairs)");
        System.out.println("  3) staff1   / Staff@123   (STAFF - Device Lookups & Requests)");
        System.out.println("  4) auditor1 / Audit@123   (AUDITOR - Dashboards & Audit Trails)");
        System.out.println("--------------------------------------------------------");

        while (!authService.isLoggedIn()) {
            System.out.print("Enter username (or 'demo' for auto-login as admin): ");
            String username = scanner.nextLine().trim();
            if ("demo".equalsIgnoreCase(username)) {
                authService.login("admin", "Admin@123");
                System.out.println("--> Logged in automatically as ADMIN (admin).\n");
                return;
            }

            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            try {
                User user = authService.login(username, password);
                System.out.printf("--> Welcome, %s! [Role: %s | Dept: %s]%n%n",
                        user.getFullName(), user.getRole(), user.getDepartment());
                return;
            } catch (UnauthorizedAccessException e) {
                System.out.println("[!] Login failed: " + e.getMessage() + ". Try again.\n");
            }
        }
    }

    private void handleSwitchUser() {
        authService.logout();
        loginFlow();
    }

    // ==========================================
    // MENU DISPLAY
    // ==========================================
    private void printMainMenu() {
        User user = authService.getCurrentUser();
        String currentRole = user != null ? user.getRole().name() : "NONE";
        String currentName = user != null ? user.getFullName() : "Guest";

        System.out.println();
        System.out.println("==========================================================================");
        System.out.printf("   BIOMEDICAL DEVICE CALIBRATION & MAINTENANCE SYSTEM  [%s: %s]%n", currentRole, currentName);
        System.out.println("==========================================================================");
        System.out.println(" [DEVICE MANAGEMENT]");
        System.out.println("   1. Register Biomedical Device");
        System.out.println("   2. View All Devices");
        System.out.println("   3. Search Device");
        System.out.println("   4. Update Device Information");
        System.out.println("   5. Retire Device");
        System.out.println(" [CALIBRATION ENGINE]");
        System.out.println("   6. View Calibration Status");
        System.out.println("   7. Perform Calibration Test");
        System.out.println("   8. View Calibration History");
        System.out.println(" [MAINTENANCE & WORK ORDERS]");
        System.out.println("   9. Create Maintenance Request");
        System.out.println("  10. Approve or Schedule Maintenance");
        System.out.println("  11. Start Maintenance");
        System.out.println("  12. Complete Maintenance");
        System.out.println("  13. Assign Technician");
        System.out.println(" [COMPLIANCE & AUDIT]");
        System.out.println("  14. Hospital Compliance Dashboard");
        System.out.println("  15. Generate Reports");
        System.out.println("  16. View Audit Logs");
        System.out.println("  17. Manage Users (ADMIN only)");
        System.out.println("  18. Switch Current User");
        System.out.println("  19. BioMed AI Copilot Assistant (Interactive Advisory)");
        System.out.println("   0. Exit");
        System.out.println("==========================================================================");
    }

    // ==========================================
    // 1. REGISTER DEVICE
    // ==========================================
    private void handleRegisterDevice() {
        System.out.println("\n--- Register New Biomedical Device ---");
        System.out.println("Select Device Hierarchy Type:");
        System.out.println("  1. Life-Support Device (e.g. Ventilator, Defibrillator, Infusion Pump)");
        System.out.println("  2. Diagnostic Device   (e.g. ECG Monitor, Ultrasound, X-Ray)");
        System.out.println("  3. Laboratory Device   (e.g. Centrifuge, Blood Gas Analyzer)");
        int typeChoice = promptInt("Choice (1-3): ", 1);

        String asset = promptString("Asset Number (e.g. AST-VENT-10): ");
        String serial = promptString("Serial Number: ");
        String name = promptString("Device Name: ");
        String mfr = promptString("Manufacturer: ");
        String model = promptString("Model Number: ");
        String dept = promptString("Department: ");
        int freq = promptInt("Calibration Frequency in Days (e.g. 180): ", 180);

        BiomedicalDevice device;
        if (typeChoice == 1) {
            int battery = promptInt("Battery Backup Minutes (e.g. 120): ", 120);
            boolean alarms = promptBoolean("Redundant Alarms Operational? (y/n): ", true);
            device = new LifeSupportDevice(null, asset, serial, name, mfr, model, dept, freq, battery, alarms);
        } else if (typeChoice == 2) {
            boolean radiation = promptBoolean("Radiation Emitting? (y/n): ", false);
            String fw = promptString("Firmware/Software Version: ");
            device = new DiagnosticDevice(null, asset, serial, name, mfr, model, dept, RiskLevel.MEDIUM, freq, radiation, fw);
        } else {
            boolean temp = promptBoolean("Requires Temperature Control? (y/n): ", false);
            double tempC = 22.0;
            if (temp) {
                tempC = promptDouble("Operating Temperature (°C): ", 4.0);
            }
            int bsl = promptInt("Biosafety Level (1-4): ", 1);
            device = new LaboratoryDevice(null, asset, serial, name, mfr, model, dept, RiskLevel.LOW, freq, temp, tempC, bsl);
        }

        BiomedicalDevice registered = deviceService.registerDevice(device);
        System.out.printf("%n[+] Device registered successfully! Assigned ID: %s | Risk Score: %.2f | PreCheck: %s%n",
                registered.getId(), registered.calculateRiskScore(), registered.passesPreCheck() ? "PASSED" : "FAILED");
    }

    // ==========================================
    // 2. VIEW ALL DEVICES
    // ==========================================
    private void handleViewAllDevices() {
        List<BiomedicalDevice> list = deviceService.findDevices();
        List<String[]> rows = new ArrayList<>();
        for (BiomedicalDevice d : list) {
            rows.add(new String[]{
                    d.getId(),
                    d.getAssetNumber(),
                    d.getName(),
                    d.getDepartment(),
                    d.getDeviceStatus().name(),
                    d.getRiskLevel().name(),
                    d.getCalibrationDueStatus(LocalDate.now()).name(),
                    DateUtils.formatDate(d.getNextCalibrationDate()),
                    d.getAssignedTechnicianId()
            });
        }

        String[] headers = {"Device ID", "Asset No", "Name", "Department", "Status", "Risk Level", "Calib Due", "Next Calib", "Tech"};
        TablePrinter.printTable("ALL REGISTERED BIOMEDICAL DEVICES (" + list.size() + ")", headers, rows);
    }

    // ==========================================
    // 3. SEARCH DEVICE (Demonstrating Polymorphic Overloading)
    // ==========================================
    private void handleSearchDevice() {
        System.out.println("\n--- Search Equipment (Polymorphic Query) ---");
        System.out.println("  1. Search by Device ID or Asset Number");
        System.out.println("  2. Search by Department (Overloaded)");
        System.out.println("  3. Search by Calibration Due Status (Overloaded)");
        System.out.println("  4. Search by Operational Status (Overloaded)");
        int choice = promptInt("Choice (1-4): ", 1);

        List<BiomedicalDevice> results = new ArrayList<>();
        if (choice == 1) {
            String query = promptString("Enter Device ID or Asset Number: ");
            deviceService.findByAssetNumber(query).ifPresent(results::add);
            if (results.isEmpty()) {
                try {
                    results.add(deviceService.getDeviceById(query));
                } catch (DeviceNotFoundException ignored) {}
            }
        } else if (choice == 2) {
            String dept = promptString("Enter Department (e.g. ICU, Radiology, Cardiology): ");
            results = deviceService.findDevices(dept);
        } else if (choice == 3) {
            System.out.println("Select Due Status: 1. VALID, 2. DUE_SOON, 3. DUE_TODAY, 4. OVERDUE, 5. NOT_REQUIRED");
            int sChoice = promptInt("Status (1-5): ", 4);
            CalibrationDueStatus status = switch (sChoice) {
                case 1 -> CalibrationDueStatus.VALID;
                case 2 -> CalibrationDueStatus.DUE_SOON;
                case 3 -> CalibrationDueStatus.DUE_TODAY;
                case 4 -> CalibrationDueStatus.OVERDUE;
                default -> CalibrationDueStatus.NOT_REQUIRED;
            };
            results = deviceService.findDevices(status);
        } else {
            System.out.println("Select Status: 1. OPERATIONAL, 2. UNDER_MAINTENANCE, 3. CALIBRATION_PENDING, 4. OUT_OF_SERVICE, 5. RETIRED");
            int stChoice = promptInt("Status (1-5): ", 1);
            DeviceStatus status = switch (stChoice) {
                case 1 -> DeviceStatus.OPERATIONAL;
                case 2 -> DeviceStatus.UNDER_MAINTENANCE;
                case 3 -> DeviceStatus.CALIBRATION_PENDING;
                case 4 -> DeviceStatus.OUT_OF_SERVICE;
                default -> DeviceStatus.RETIRED;
            };
            results = deviceService.findDevices(status);
        }

        List<String[]> rows = new ArrayList<>();
        for (BiomedicalDevice d : results) {
            rows.add(new String[]{
                    d.getId(),
                    d.getAssetNumber(),
                    d.getName(),
                    d.getDepartment(),
                    d.getDeviceStatus().name(),
                    d.getCalibrationDueStatus(LocalDate.now()).name(),
                    String.format("%.2f", d.calculateRiskScore()),
                    d.passesPreCheck() ? "PASS" : "FAIL"
            });
        }
        String[] headers = {"Device ID", "Asset No", "Name", "Department", "Status", "Calib Due", "Risk Score", "PreCheck"};
        TablePrinter.printTable("SEARCH RESULTS (" + results.size() + " matches)", headers, rows);
    }

    // ==========================================
    // 4. UPDATE DEVICE
    // ==========================================
    private void handleUpdateDevice() {
        System.out.println("\n--- Update Device Information ---");
        String id = promptString("Enter Device ID: ");
        BiomedicalDevice d = deviceService.getDeviceById(id);

        System.out.println("Current Name: " + d.getName());
        String name = promptStringAllowBlank("New Name (leave blank to keep current): ");
        if (!name.isEmpty()) d.setName(name);

        System.out.println("Current Department: " + d.getDepartment());
        String dept = promptStringAllowBlank("New Department (leave blank to keep current): ");
        if (!dept.isEmpty()) d.setDepartment(dept);

        System.out.println("Current Calib Frequency (Days): " + d.getCalibrationFrequencyDays());
        String freqStr = promptStringAllowBlank("New Frequency (leave blank to keep current): ");
        if (!freqStr.isEmpty()) {
            try {
                d.setCalibrationFrequencyDays(Integer.parseInt(freqStr));
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid number. Keeping current frequency.");
            }
        }

        deviceService.updateDevice(d);
        System.out.println("[+] Device updated successfully.");
    }

    // ==========================================
    // 5. RETIRE DEVICE
    // ==========================================
    private void handleRetireDevice() {
        System.out.println("\n--- Retire / Decommission Biomedical Device ---");
        String id = promptString("Enter Device ID: ");
        String reason = promptString("Enter Reason for Decommissioning: ");
        BiomedicalDevice retired = deviceService.retireDevice(id, reason);
        System.out.printf("[+] Device %s (%s) has been permanently marked RETIRED.%n", retired.getId(), retired.getName());
    }

    // ==========================================
    // 6. VIEW CALIBRATION STATUS
    // ==========================================
    private void handleViewCalibrationStatus() {
        String id = promptString("Enter Device ID: ");
        BiomedicalDevice d = deviceService.getDeviceById(id);
        LocalDate today = LocalDate.now();

        System.out.println("\n========================================================");
        System.out.println("               DEVICE CALIBRATION DOSSIER               ");
        System.out.println("========================================================");
        System.out.println("Device ID        : " + d.getId());
        System.out.println("Asset Number     : " + d.getAssetNumber());
        System.out.println("Device Name      : " + d.getName());
        System.out.println("Department       : " + d.getDepartment());
        System.out.println("Device Status    : " + d.getDeviceStatus());
        System.out.println("Risk Level       : " + d.getRiskLevel() + " (Score: " + d.calculateRiskScore() + ")");
        System.out.println("Calib Required   : " + d.isCalibrationRequired());
        System.out.println("Frequency        : Every " + d.getCalibrationFrequencyDays() + " days");
        System.out.println("Last Calibrated  : " + DateUtils.formatDate(d.getLastCalibrationDate()));
        System.out.println("Next Due Date    : " + DateUtils.formatDate(d.getNextCalibrationDate()));
        System.out.println("Due Status       : " + d.getCalibrationDueStatus(today));
        System.out.println("Pre-Check Result : " + (d.passesPreCheck() ? "PASSED" : "FAILED"));
        System.out.println("========================================================");
    }

    // ==========================================
    // 7. PERFORM CALIBRATION TEST
    // ==========================================
    private void handlePerformCalibration() {
        System.out.println("\n--- Perform Simulated Calibration Test ---");
        String deviceId = promptString("Enter Target Device ID: ");
        BiomedicalDevice device = deviceService.getDeviceById(deviceId);

        if (device.getDeviceStatus() == DeviceStatus.RETIRED) {
            System.out.println("[SAFETY VIOLATION] Cannot calibrate a retired device.");
            return;
        }

        String techId = authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : "TECH-101";
        System.out.println("Certifying Technician: " + techId);

        String equipment = promptString("Calibration Standard Equipment (e.g. Fluke VT900A, Multimeter): ");
        String notes = promptString("Verification Notes: ");

        List<CalibrationMeasurement> measurements = new ArrayList<>();
        System.out.println("\nEnter Calibration Parameter Measurements (enter blank name when finished, min 1):");

        int count = 1;
        while (true) {
            System.out.printf("%n[Parameter #%d]%n", count);
            String param = promptStringAllowBlank("Parameter Name (or press Enter to finish): ");
            if (param.isEmpty()) {
                if (measurements.isEmpty()) {
                    System.out.println("[!] At least 1 measurement is required. Please specify a parameter.");
                    continue;
                }
                break;
            }

            double nominal = promptDouble("Nominal / Expected Value: ", 100.0);
            double measured = promptDouble("Measured Value: ", 100.0);
            String unit = promptString("Unit of Measurement (e.g. mL, cmH2O, mV, Joules, RPM): ");
            double tol = promptDouble("Allowed Tolerance (±): ", 5.0);

            CalibrationMeasurement m = new CalibrationMeasurement(param, nominal, measured, unit, tol);
            measurements.add(m);
            System.out.printf("  -> Deviation: %.3f %s (Allowed: ±%.3f). Result: [%s]%n",
                    m.getDeviation(), m.getUnit(), m.getAllowedTolerance(), m.isPassed() ? "PASS" : "FAIL");
            count++;
        }

        CalibrationRecord record = calibrationService.performCalibration(deviceId, techId, measurements, equipment, notes);

        System.out.println("\n========================================================");
        System.out.println("               CALIBRATION TEST SUMMARY                 ");
        System.out.println("========================================================");
        System.out.println("Calibration Record ID : " + record.getId());
        System.out.println("Overall Test Result   : " + record.getStatus());
        System.out.println("Device New Status     : " + device.getDeviceStatus());

        if (record.getStatus() == CalibrationStatus.PASSED) {
            System.out.println("Certificate Number    : " + record.getCertificate().getCertificateNumber());
            System.out.println("Next Calibration Due  : " + DateUtils.formatDate(device.getNextCalibrationDate()));
            System.out.println("[+] SAFETY VERIFICATION PASSED: Device is operational for clinical use.");
        } else {
            System.out.println("Failure Reason        : " + record.getFailureReason());
            System.out.println("[!] SAFETY ALERT: Equipment failed tolerance limits!");
            System.out.println("[!] Device has been locked OUT OF SERVICE. Normal use prohibited.");
        }
        System.out.println("========================================================");
    }

    // ==========================================
    // 8. VIEW CALIBRATION HISTORY
    // ==========================================
    private void handleViewCalibrationHistory() {
        String deviceId = promptString("Enter Device ID: ");
        List<CalibrationRecord> history = calibrationService.getCalibrationHistory(deviceId);

        List<String[]> rows = new ArrayList<>();
        for (CalibrationRecord r : history) {
            rows.add(new String[]{
                    r.getId(),
                    DateUtils.formatDate(r.getCalibrationDate()),
                    r.getStatus().name(),
                    r.getTechnicianId(),
                    r.getCertificate() != null ? r.getCertificate().getCertificateNumber() : "NONE",
                    String.valueOf(r.getMeasurements().size()),
                    r.getFailureReason() != null ? r.getFailureReason() : "Compliant"
            });
        }

        String[] headers = {"Calib ID", "Date", "Status", "Tech ID", "Cert No", "Meas Count", "Notes"};
        TablePrinter.printTable("CALIBRATION HISTORY FOR DEVICE " + deviceId.toUpperCase(), headers, rows);
    }

    // ==========================================
    // 9. CREATE MAINTENANCE REQUEST
    // ==========================================
    private void handleCreateMaintenanceRequest() {
        System.out.println("\n--- Create Maintenance Work Order Request ---");
        String deviceId = promptString("Enter Device ID: ");
        System.out.println("Select Maintenance Type:");
        System.out.println("  1. PREVENTIVE (Routine inspection, filter changes, cleaning)");
        System.out.println("  2. CORRECTIVE (Repair of functional defect or wear)");
        System.out.println("  3. EMERGENCY  (Immediate clinical fault or alarm condition)");
        int tChoice = promptInt("Choice (1-3): ", 2);

        MaintenanceType type = switch (tChoice) {
            case 1 -> MaintenanceType.PREVENTIVE;
            case 3 -> MaintenanceType.EMERGENCY;
            default -> MaintenanceType.CORRECTIVE;
        };

        String desc = promptString("Describe the issue or maintenance scope: ");
        MaintenanceRequest req = maintenanceService.createRequest(deviceId, type, desc);
        System.out.printf("[+] Maintenance request created! ID: %s | Status: %s%n", req.getId(), req.getStatus());
    }

    // ==========================================
    // 10. APPROVE OR SCHEDULE MAINTENANCE
    // ==========================================
    private void handleApproveScheduleMaintenance() {
        System.out.println("\n--- Approve or Schedule Maintenance (ADMIN) ---");
        String reqId = promptString("Enter Maintenance Request ID (e.g. REQ-4001): ");
        String techId = promptString("Assign to Technician ID (e.g. TECH-101): ");
        int days = promptInt("Schedule for how many days from now? (0 for today): ", 0);

        LocalDateTime scheduledDate = LocalDateTime.now().plusDays(days);
        maintenanceService.approveAndScheduleRequest(reqId, techId, scheduledDate);
        System.out.println("[+] Request approved and scheduled for " + DateUtils.formatDateTime(scheduledDate));
    }

    // ==========================================
    // 11. START MAINTENANCE
    // ==========================================
    private void handleStartMaintenance() {
        System.out.println("\n--- Start Maintenance Servicing ---");
        String reqId = promptString("Enter Maintenance Request ID: ");
        String techId = authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : "TECH-101";

        AbstractMaintenanceRecord record = maintenanceService.startMaintenance(reqId, techId);
        System.out.printf("[+] Maintenance work started! Work Order ID: %s | Device Status: UNDER_MAINTENANCE%n", record.getId());
    }

    // ==========================================
    // 12. COMPLETE MAINTENANCE
    // ==========================================
    private void handleCompleteMaintenance() {
        System.out.println("\n--- Complete Maintenance Work Order ---");
        String recordId = promptString("Enter Maintenance Record ID (e.g. MNT-3001): ");

        double costVal = promptDouble("Total Maintenance Cost ($): ", 150.0);
        BigDecimal cost = BigDecimal.valueOf(costVal);
        double downtime = promptDouble("Total Downtime Hours: ", 2.5);

        String partsInput = promptStringAllowBlank("Parts Replaced (comma separated, e.g. O2 Sensor, HEPA Filter): ");
        List<String> parts = new ArrayList<>();
        if (!partsInput.isEmpty()) {
            for (String p : partsInput.split(",")) parts.add(p.trim());
        }

        String notes = promptString("Resolution / Service Summary: ");
        maintenanceService.completeMaintenance(recordId, cost, downtime, parts, List.of("Safety Inspection", "Power Verification"), notes);

        System.out.println("[+] Maintenance work order marked COMPLETED.");
        System.out.println("[!] NOTE: If safety-critical parts were repaired or calibrated tolerance had failed,");
        System.out.println("    the device remains CALIBRATION_PENDING until a formal calibration test passes.");
    }

    // ==========================================
    // 13. ASSIGN TECHNICIAN
    // ==========================================
    private void handleAssignTechnician() {
        System.out.println("\n--- Assign Technician to Equipment ---");
        String devId = promptString("Enter Device ID: ");
        String techId = promptString("Enter Technician ID (e.g. TECH-101): ");
        deviceService.assignTechnician(devId, techId);
        System.out.printf("[+] Technician %s assigned to device %s.%n", techId, devId);
    }

    // ==========================================
    // 14. COMPLIANCE DASHBOARD
    // ==========================================
    private void handleComplianceDashboard() {
        ComplianceService.ComplianceDashboardData kpis = complianceService.getDashboardMetrics();

        System.out.println("\n==========================================================================");
        System.out.println("                 HOSPITAL BIOMEDICAL COMPLIANCE DASHBOARD                 ");
        System.out.println("==========================================================================");
        System.out.printf(" Total Registered Devices          : %-5d | Active (Operational) : %d%n",
                kpis.totalDevices(), kpis.activeDevices());
        System.out.printf(" Devices Under Maintenance         : %-5d | Out of Service       : %d%n",
                kpis.underMaintenanceDevices(), kpis.outOfServiceDevices());
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" CALIBRATION METRICS:");
        System.out.printf("   - Valid Calibrations            : %-5d | - Due Soon (<= 7d)  : %d%n",
                kpis.validCalibrations(), kpis.dueSoonCalibrations());
        System.out.printf("   - Due Today                     : %-5d | - Overdue            : %d%n",
                kpis.dueTodayCalibrations(), kpis.overdueCalibrations());
        System.out.printf("   - Calibration Exempt            : %-5d | - Failed Calibrations: %d%n",
                kpis.notRequiredCalibrations(), kpis.failedCalibrations());
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" KEY SAFETY COMPLIANCE KPI:");
        System.out.printf("   >>> Calibration Compliance Rate : %.1f%% (%d of %d devices compliant) <<<%n",
                kpis.calibrationCompliancePercentage(), kpis.compliantDevicesCount(), kpis.subjectDevicesCount());
        System.out.println("   Formula: (Valid + Due Soon) / (Total Requiring Periodic Calibration) * 100");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" MAINTENANCE & DOWNTIME OPERATIONS:");
        System.out.printf("   - Open Work Orders              : %-5d | - Completed Jobs    : %d%n",
                kpis.openMaintenanceRequests(), kpis.completedMaintenanceJobs());
        System.out.printf("   - Total Maintenance Cost        : $%-10s | - Cumulative Downtime: %.1f hrs%n",
                kpis.totalMaintenanceCost().toPlainString(), kpis.totalDowntimeHours());
        System.out.println("==========================================================================");
        System.out.println("[DISCLAIMER] Educational safety metrics simulation. Not a clinical ISO 17025 certificate.");
    }

    // ==========================================
    // 15. GENERATE REPORTS
    // ==========================================
    private void handleGenerateReports() {
        System.out.println("\n--- Generate and Export Reports ---");
        System.out.println("  1. Device Inventory Report (CSV)");
        System.out.println("  2. Calibration History Report (CSV)");
        System.out.println("  3. Overdue Calibrations Report (CSV)");
        System.out.println("  4. Maintenance History Report (CSV)");
        System.out.println("  5. Compliance Summary Report (TXT)");
        System.out.println("  6. Audit Log Export (CSV)");
        System.out.println("  7. Export ALL Reports");
        int rChoice = promptInt("Choice (1-7): ", 7);

        try {
            switch (rChoice) {
                case 1 -> {
                    Path p = reportService.exportDeviceInventoryReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 2 -> {
                    Path p = reportService.exportCalibrationHistoryReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 3 -> {
                    Path p = reportService.exportOverdueCalibrationReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 4 -> {
                    Path p = reportService.exportMaintenanceHistoryReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 5 -> {
                    Path p = reportService.exportComplianceSummaryReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 6 -> {
                    Path p = reportService.exportAuditLogReport();
                    System.out.println("[+] Exported: " + p.toAbsolutePath());
                }
                case 7 -> {
                    System.out.println("[+] Exported: " + reportService.exportDeviceInventoryReport().toAbsolutePath());
                    System.out.println("[+] Exported: " + reportService.exportCalibrationHistoryReport().toAbsolutePath());
                    System.out.println("[+] Exported: " + reportService.exportOverdueCalibrationReport().toAbsolutePath());
                    System.out.println("[+] Exported: " + reportService.exportMaintenanceHistoryReport().toAbsolutePath());
                    System.out.println("[+] Exported: " + reportService.exportComplianceSummaryReport().toAbsolutePath());
                    System.out.println("[+] Exported: " + reportService.exportAuditLogReport().toAbsolutePath());
                }
                default -> System.out.println("[!] Invalid choice.");
            }
        } catch (Exception e) {
            System.out.println("[!] Failed to generate report: " + e.getMessage());
        }
    }

    // ==========================================
    // 16. VIEW AUDIT LOGS
    // ==========================================
    private void handleViewAuditLogs() {
        authService.checkPermission(UserRole.AUDITOR, UserRole.ADMIN);
        List<com.hospital.biomedical.model.audit.AuditLog> logs = auditService.getAllLogs();

        List<String[]> rows = new ArrayList<>();
        // Display recent 25 logs
        int start = Math.max(0, logs.size() - 25);
        for (int i = start; i < logs.size(); i++) {
            com.hospital.biomedical.model.audit.AuditLog l = logs.get(i);
            rows.add(new String[]{
                    l.getId(),
                    DateUtils.formatDateTime(l.getCreatedAt()),
                    l.getActingUsername(),
                    l.getActionType(),
                    l.getTargetEntity(),
                    l.getTargetId(),
                    l.getDetails()
            });
        }

        String[] headers = {"Event ID", "Timestamp", "User", "Action", "Target", "Target ID", "Details"};
        TablePrinter.printTable("RECENT AUDIT TRAIL LOGS (" + rows.size() + " of " + logs.size() + ")", headers, rows);
    }

    // ==========================================
    // 17. MANAGE USERS
    // ==========================================
    private void handleManageUsers() {
        authService.checkPermission(UserRole.ADMIN);
        System.out.println("\n--- User Management (ADMIN) ---");
        System.out.println("  1. List All Users");
        System.out.println("  2. Reset Demonstration Accounts");
        int uChoice = promptInt("Choice (1-2): ", 1);

        if (uChoice == 1) {
            List<User> users = userRepository.findAll();
            List<String[]> rows = new ArrayList<>();
            for (User u : users) {
                rows.add(new String[]{
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getRole().name(),
                        u.getDepartment(),
                        u.isActive() ? "Active" : "Disabled",
                        (u instanceof Technician t) ? t.getCertificationNumber() : "N/A"
                });
            }
            String[] headers = {"User ID", "Username", "Full Name", "Role", "Department", "Status", "Cert No"};
            TablePrinter.printTable("SYSTEM USER REGISTRY", headers, rows);
        } else if (uChoice == 2) {
            userRepository.resetToDefaultAccounts();
            System.out.println("[+] Default demonstration user accounts restored.");
        }
    }

    // ==========================================
    // PROMPT & VALIDATION HELPERS
    // ==========================================
    private void printBanner() {
        System.out.println("==========================================================================");
        System.out.println("   BIOMEDICAL DEVICE CALIBRATION & MAINTENANCE MANAGEMENT SYSTEM          ");
        System.out.println("   Enterprise Java OOP Simulation for Clinical Engineering                ");
        System.out.println("==========================================================================");
    }

    // ==========================================
    // 19. BIOMED AI ASSISTANT COPILOT
    // ==========================================
    private void handleAiAssistant() {
        System.out.println("\n========================================================");
        System.out.println("   ⚡ BIOMED AI ASSISTANT COPILOT - CLINICAL ADVISORY    ");
        System.out.println("========================================================");
        System.out.println("Ask any question regarding device telemetry, calibration math,");
        System.out.println("tolerance thresholds, maintenance safety, or regulatory rules.");
        System.out.println("Type 'exit' to return to the main menu.\n");
        System.out.println("Suggested prompts: 'Show overdue devices', 'Tolerance for Defibrillator',");
        System.out.println("'Compliance health check', 'Explain post-maintenance interlock'\n");

        while (true) {
            System.out.print("[BioMed AI] Ask a question: ");
            String q = scanner.nextLine().trim();
            if (q.equalsIgnoreCase("exit") || q.equalsIgnoreCase("quit") || q.isEmpty()) {
                break;
            }
            var resp = aiAssistantService.askAi(q, authService.getCurrentUser());
            System.out.printf("%n--- [%s] ---%n", resp.engine());
            System.out.println(resp.reply());
            System.out.println("\n--------------------------------------------------------\n");
        }
    }

    private String promptString(String message) {
        while (true) {
            System.out.print(message);
            String val = scanner.nextLine().trim();
            if (!val.isEmpty()) return val;
            System.out.println("[!] Input cannot be blank.");
        }
    }

    private String promptStringAllowBlank(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private int promptInt(String message, int defaultVal) {
        System.out.print(message);
        String val = scanner.nextLine().trim();
        if (val.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            System.out.println("[!] Invalid number format. Defaulting to: " + defaultVal);
            return defaultVal;
        }
    }

    private double promptDouble(String message, double defaultVal) {
        System.out.print(message);
        String val = scanner.nextLine().trim();
        if (val.isEmpty()) return defaultVal;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            System.out.println("[!] Invalid numeric format. Defaulting to: " + defaultVal);
            return defaultVal;
        }
    }

    private boolean promptBoolean(String message, boolean defaultVal) {
        System.out.print(message);
        String val = scanner.nextLine().trim().toLowerCase();
        if (val.isEmpty()) return defaultVal;
        return val.startsWith("y") || "true".equals(val) || "1".equals(val);
    }
}
