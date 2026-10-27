package com.hospital.biomedical.service;

import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LaboratoryDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.MaintenanceType;
import com.hospital.biomedical.model.enums.RiskLevel;
import com.hospital.biomedical.model.user.User;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds initial realistic biomedical devices, calibration history, and work orders
 * if the workspace is freshly initialized.
 */
public class DataSeeder {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final CalibrationService calibrationService;
    private final MaintenanceService maintenanceService;
    private final AuthService authService;

    public DataSeeder(DeviceRepository deviceRepository, UserRepository userRepository,
                      CalibrationService calibrationService, MaintenanceService maintenanceService,
                      AuthService authService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.calibrationService = calibrationService;
        this.maintenanceService = maintenanceService;
        this.authService = authService;
    }

    public void seedIfEmpty() {
        if (deviceRepository.count() > 0) {
            return; // Data already exists
        }

        // Temporarily elevate context as admin to seed demo records
        User admin = userRepository.findByUsername("admin").orElse(null);
        authService.setCurrentUser(admin);

        LocalDate today = LocalDate.now();

        // 1. Life Support: Mechanical Ventilator (Valid calibration)
        LifeSupportDevice vent = new LifeSupportDevice("DEV-1001", "AST-VENT-01", "SN-PB980-4102",
                "Puritan Bennett 980 Ventilator", "Medtronic", "PB980", "Intensive Care Unit (ICU)",
                180, 180, true);
        vent.setAssignedTechnicianId("TECH-101");
        deviceRepository.save(vent);

        // Perform successful baseline calibration for ventilator
        calibrationService.performCalibration("DEV-1001", "TECH-101", List.of(
                new CalibrationMeasurement("Tidal Volume Output", 500.0, 503.2, "mL", 15.0),
                new CalibrationMeasurement("PEEP Pressure", 10.0, 10.1, "cmH2O", 1.0),
                new CalibrationMeasurement("FiO2 Oxygen Concentration", 50.0, 49.8, "%", 2.0)
        ), "Fluke VT900A Gas Flow Analyzer", "Annual preventive calibration passed", today.minusMonths(2));

        // 2. Life Support: Biphasic Defibrillator (Due soon: next due in 4 days)
        LifeSupportDevice defib = new LifeSupportDevice("DEV-1002", "AST-DEFIB-02", "SN-ZOLL-8821",
                "Zoll R Series Defibrillator", "Zoll Medical", "R-Series Plus", "Emergency Department",
                90, 120, true);
        defib.setAssignedTechnicianId("TECH-101");
        deviceRepository.save(defib);

        calibrationService.performCalibration("DEV-1002", "TECH-101", List.of(
                new CalibrationMeasurement("Energy Discharge (200J)", 200.0, 198.5, "Joules", 6.0),
                new CalibrationMeasurement("Pacer Current (100mA)", 100.0, 99.4, "mA", 3.0),
                new CalibrationMeasurement("Charge Time to Max Energy", 4.5, 4.2, "seconds", 1.0)
        ), "Fluke Impulse 7000DP Defibrillator Analyzer", "Routine quarterly calibration", today.minusDays(86));

        // 3. Diagnostic: 12-Lead ECG Patient Monitor (Valid calibration)
        DiagnosticDevice ecg = new DiagnosticDevice("DEV-1003", "AST-ECG-03", "SN-PHIL-1920",
                "IntelliVue MX800 Patient Monitor", "Philips Healthcare", "MX800", "Cardiology",
                RiskLevel.MEDIUM, 365, false, "v4.2.1");
        ecg.setAssignedTechnicianId("TECH-102");
        deviceRepository.save(ecg);

        calibrationService.performCalibration("DEV-1003", "TECH-102", List.of(
                new CalibrationMeasurement("HR Amplitude (1mV pulse)", 1.0, 1.01, "mV", 0.05),
                new CalibrationMeasurement("SpO2 Optical Calibration", 98.0, 98.2, "%", 1.5),
                new CalibrationMeasurement("NIBP Systolic Simulation", 120.0, 121.0, "mmHg", 3.0)
        ), "ProSim 8 Vital Signs Simulator", "Annual performance verification", today.minusMonths(5));

        // 4. Diagnostic: Mobile Digital X-Ray System (OVERDUE calibration!)
        DiagnosticDevice xray = new DiagnosticDevice("DEV-1004", "AST-XRAY-04", "SN-SIEM-7733",
                "Mobilett Elara Max Mobile X-Ray", "Siemens Healthineers", "Elara Max", "Radiology",
                RiskLevel.HIGH, 180, true, "v3.0.0");
        xray.setAssignedTechnicianId("TECH-102");
        deviceRepository.save(xray);

        calibrationService.performCalibration("DEV-1004", "TECH-102", List.of(
                new CalibrationMeasurement("kVp Output (80 kVp)", 80.0, 80.4, "kVp", 2.0),
                new CalibrationMeasurement("mAs Accuracy (20 mAs)", 20.0, 19.8, "mAs", 1.0),
                new CalibrationMeasurement("Radiation Timer (100 ms)", 100.0, 99.2, "ms", 2.0)
        ), "Radcal Accu-Gold+ Dosimetry System", "Past inspection", today.minusMonths(8)); // Overdue!

        // 5. Diagnostic: Ultrasound Imaging Console (Valid calibration)
        DiagnosticDevice ultrasound = new DiagnosticDevice("DEV-1005", "AST-US-05", "SN-GELOG-5510",
                "LOGIQ E10 Ultrasound Console", "GE Healthcare", "LOGIQ E10", "Obstetrics & Gynecology",
                RiskLevel.MEDIUM, 365, false, "v2.4.0");
        ultrasound.setAssignedTechnicianId("TECH-102");
        deviceRepository.save(ultrasound);

        calibrationService.performCalibration("DEV-1005", "TECH-102", List.of(
                new CalibrationMeasurement("Transducer Acoustic Output", 10.0, 9.9, "mW/cm2", 0.5),
                new CalibrationMeasurement("Depth Caliper Distance (100mm)", 100.0, 100.2, "mm", 1.0)
        ), "Multi-Tissue Ultrasound Phantom 040GSE", "Annual phantom verification", today.minusMonths(1));

        // 6. Laboratory: High-Speed Refrigerated Centrifuge (Valid calibration)
        LaboratoryDevice centrifuge = new LaboratoryDevice("DEV-1006", "AST-CENT-06", "SN-EPP-9912",
                "5810 R Multipurpose Centrifuge", "Eppendorf", "5810 R", "Central Pathology Lab",
                RiskLevel.LOW, 180, true, 4.0, 2);
        centrifuge.setAssignedTechnicianId("TECH-101");
        deviceRepository.save(centrifuge);

        calibrationService.performCalibration("DEV-1006", "TECH-101", List.of(
                new CalibrationMeasurement("Rotational Speed (4000 RPM)", 4000.0, 4005.0, "RPM", 25.0),
                new CalibrationMeasurement("Chamber Temperature (4.0 C)", 4.0, 4.2, "deg C", 0.8),
                new CalibrationMeasurement("Run Timer Accuracy (30 min)", 1800.0, 1798.5, "seconds", 5.0)
        ), "Optical Tachometer & NIST Thermometer", "Semi-annual speed and temperature calibration", today.minusMonths(2));

        // 7. Life Support: Volumetric Infusion Pump (Calibration required, never calibrated -> OVERDUE)
        LifeSupportDevice pump = new LifeSupportDevice("DEV-1007", "AST-PUMP-07", "SN-BD-6611",
                "Alaris 8015 Infusion System", "Becton Dickinson", "Alaris 8015", "Pediatric Ward",
                180, 240, true);
        pump.setAssignedTechnicianId("TECH-101");
        deviceRepository.save(pump);

        // 8. Laboratory: Blood Gas Analyzer (Has open maintenance request)
        LaboratoryDevice bga = new LaboratoryDevice("DEV-1008", "AST-BGA-08", "SN-RAD-3301",
                "ABL90 FLEX Blood Gas Analyzer", "Radiometer", "ABL90", "Stat Laboratory",
                RiskLevel.LOW, 90, false, 22.0, 1);
        bga.setAssignedTechnicianId("TECH-102");
        deviceRepository.save(bga);

        // Create sample maintenance request and complete a sample job
        maintenanceService.createRequest("DEV-1008", MaintenanceType.PREVENTIVE, "Routine sensor cassette replacement and fluidic flush");

        // Clear elevated auth session
        authService.logout();
    }
}
