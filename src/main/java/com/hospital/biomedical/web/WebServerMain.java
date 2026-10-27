package com.hospital.biomedical.web;

import com.hospital.biomedical.repository.*;
import com.hospital.biomedical.service.*;

/**
 * Dedicated launcher for the Biomedical Web Application HTTP Server.
 */
public class WebServerMain {
    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   BIOMEDICAL EQUIPMENT MANAGEMENT - WEB APPLICATION LAUNCHER             ");
        System.out.println("==========================================================================");

        DeviceRepository deviceRepository = new DeviceRepository();
        UserRepository userRepository = new UserRepository();
        CalibrationRepository calibrationRepository = new CalibrationRepository();
        MaintenanceRepository maintenanceRepository = new MaintenanceRepository();
        AuditRepository auditRepository = new AuditRepository();

        AuditService auditService = new AuditService(auditRepository);
        AuthService authService = new AuthService(userRepository, auditService);
        DeviceService deviceService = new DeviceService(deviceRepository, authService, auditService);
        CalibrationService calibrationService = new CalibrationService(calibrationRepository, deviceRepository, authService, auditService);
        MaintenanceService maintenanceService = new MaintenanceService(maintenanceRepository, deviceRepository, authService, auditService);
        ComplianceService complianceService = new ComplianceService(deviceRepository, calibrationRepository, maintenanceRepository);
        ReportService reportService = new ReportService(deviceService, calibrationService, maintenanceService, complianceService, auditService);

        // Seed data if empty
        DataSeeder seeder = new DataSeeder(deviceRepository, userRepository, calibrationService, maintenanceService, authService);
        seeder.seedIfEmpty();

        BiomedicalAiAssistantService aiAssistantService = new BiomedicalAiAssistantService(
                deviceService, complianceService, calibrationService, maintenanceService);

        try {
            WebServer webServer = new WebServer(deviceService, calibrationService, maintenanceService,
                    complianceService, authService, auditService, reportService, aiAssistantService);
            int port = webServer.start();

            String httpUrl = "http://localhost:" + port + "/";
            System.out.println();
            System.out.println(">>> ---------------------------------------------------------------- <<<");
            System.out.println(">>> HTTP WEB INTERFACE IS READY!                                     <<<");
            System.out.println(">>> ACCESS LINK: " + httpUrl + "                             <<<");
            System.out.println(">>> ---------------------------------------------------------------- <<<");
            System.out.println();
            System.out.println("[*] Server is running. Press Ctrl+C in terminal to stop.");

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[!] Failed to launch Web Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
