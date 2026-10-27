package com.hospital.biomedical;

import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LaboratoryDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.RiskLevel;
import com.hospital.biomedical.repository.CalibrationRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.MaintenanceRepository;
import com.hospital.biomedical.service.ComplianceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for Hospital Compliance Dashboard KPI calculations.
 */
public class ComplianceTest {

    @TempDir
    Path tempDir;

    private DeviceRepository deviceRepository;
    private CalibrationRepository calibrationRepository;
    private MaintenanceRepository maintenanceRepository;
    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        deviceRepository = new DeviceRepository(tempDir.resolve("devices.csv"));
        calibrationRepository = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("meas.csv"));
        maintenanceRepository = new MaintenanceRepository(tempDir.resolve("maint.csv"), tempDir.resolve("req.csv"));
        complianceService = new ComplianceService(deviceRepository, calibrationRepository, maintenanceRepository);
    }

    @Test
    @DisplayName("Empty repository returns 100% compliance without division-by-zero")
    void testEmptyRepositoryCompliance() {
        ComplianceService.ComplianceDashboardData data = complianceService.getDashboardMetrics();
        assertEquals(0, data.totalDevices());
        assertEquals(100.0, data.calibrationCompliancePercentage());
    }

    @Test
    @DisplayName("Accurate calculation of compliance percentage excluding exempt devices")
    void testCompliancePercentageCalculation() {
        LocalDate refDate = LocalDate.of(2026, 7, 1);

        // Device 1: Requires calib -> Valid (due in 30 days) -> COMPLIANT
        LifeSupportDevice d1 = new LifeSupportDevice("D1", "AST1", "SN1", "Dev 1", "OEM", "M1", "ICU", 180, 120, true);
        d1.setNextCalibrationDate(refDate.plusDays(30));
        deviceRepository.save(d1);

        // Device 2: Requires calib -> Due Soon (due in 4 days) -> COMPLIANT
        DiagnosticDevice d2 = new DiagnosticDevice("D2", "AST2", "SN2", "Dev 2", "OEM", "M2", "Rad", RiskLevel.MEDIUM, 180, false, "v1");
        d2.setNextCalibrationDate(refDate.plusDays(4));
        deviceRepository.save(d2);

        // Device 3: Requires calib -> Overdue (due 5 days ago) -> NON-COMPLIANT
        LifeSupportDevice d3 = new LifeSupportDevice("D3", "AST3", "SN3", "Dev 3", "OEM", "M3", "ICU", 180, 120, true);
        d3.setNextCalibrationDate(refDate.minusDays(5));
        deviceRepository.save(d3);

        // Device 4: Calibration NOT required -> EXCLUDED from denominator!
        LaboratoryDevice d4 = new LaboratoryDevice("D4", "AST4", "SN4", "Dev 4", "OEM", "M4", "Lab", RiskLevel.LOW, 180, false, 22.0, 1);
        d4.setCalibrationRequired(false);
        deviceRepository.save(d4);

        ComplianceService.ComplianceDashboardData data = complianceService.getDashboardMetrics(refDate);

        assertEquals(4, data.totalDevices());
        assertEquals(3, data.subjectDevicesCount(), "Only 3 devices require calibration");
        assertEquals(2, data.compliantDevicesCount(), "Only D1 and D2 are compliant (Valid or Due Soon)");

        // 2 compliant out of 3 subject = 66.7%
        assertEquals(66.7, data.calibrationCompliancePercentage(), 0.1);
        assertEquals(1, data.notRequiredCalibrations());
        assertEquals(1, data.overdueCalibrations());
        assertEquals(1, data.dueSoonCalibrations());
        assertEquals(1, data.validCalibrations());
    }
}
