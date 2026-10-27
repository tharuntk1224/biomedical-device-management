package com.hospital.biomedical;

import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.CalibrationStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.repository.CalibrationRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.UserRepository;
import com.hospital.biomedical.service.AuditService;
import com.hospital.biomedical.service.AuthService;
import com.hospital.biomedical.service.CalibrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for Calibration Engine, tolerance calculations, and safety lockouts.
 */
public class CalibrationTest {

    @TempDir
    Path tempDir;

    private DeviceRepository deviceRepository;
    private CalibrationRepository calibrationRepository;
    private CalibrationService calibrationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        deviceRepository = new DeviceRepository(tempDir.resolve("devices.csv"));
        calibrationRepository = new CalibrationRepository(tempDir.resolve("cal.csv"), tempDir.resolve("meas.csv"));
        UserRepository userRepository = new UserRepository(tempDir.resolve("users.csv"));
        AuditService auditService = new AuditService(new com.hospital.biomedical.repository.AuditRepository(tempDir.resolve("audit.csv")));
        authService = new AuthService(userRepository, auditService);

        calibrationService = new CalibrationService(calibrationRepository, deviceRepository, authService, auditService);

        // Login as tech
        authService.login("tech1", "Tech@123");
    }

    @Test
    @DisplayName("Calibration measurement within allowed tolerance passes")
    void testMeasurementWithinTolerance() {
        CalibrationMeasurement m = new CalibrationMeasurement("Pressure", 100.0, 102.5, "psi", 5.0);
        assertEquals(2.5, m.getDeviation(), 0.001);
        assertTrue(m.isPassed());
    }

    @Test
    @DisplayName("Calibration measurement exceeding allowed tolerance fails")
    void testMeasurementOutsideTolerance() {
        CalibrationMeasurement m = new CalibrationMeasurement("Pressure", 100.0, 108.0, "psi", 5.0);
        assertEquals(8.0, m.getDeviation(), 0.001);
        assertFalse(m.isPassed());
    }

    @Test
    @DisplayName("Calibration record with no measurements fails evaluation")
    void testCalibrationWithNoMeasurementsFails() {
        CalibrationRecord record = new CalibrationRecord("CAL-999", "DEV-1001", "TECH-101", LocalDate.now(), "Tool", "Notes");
        boolean passed = record.evaluate();
        assertFalse(passed);
        assertEquals(CalibrationStatus.FAILED, record.getStatus());
        assertNotNull(record.getFailureReason());
        assertNull(record.getCertificate(), "Failed calibration must never generate certificate.");
    }

    @Test
    @DisplayName("Successful calibration sets next due date and issues certificate")
    void testSuccessfulCalibrationAndNextDueDateCalculation() {
        LifeSupportDevice vent = new LifeSupportDevice("DEV-TEST-01", "AST-VENT-99", "SN-99",
                "Ventilator", "OEM", "V1", "ICU", 90, 120, true);
        deviceRepository.save(vent);

        LocalDate testDate = LocalDate.of(2026, 5, 1);
        List<CalibrationMeasurement> measurements = List.of(
                new CalibrationMeasurement("Tidal Volume", 500.0, 502.0, "mL", 10.0),
                new CalibrationMeasurement("Pressure", 20.0, 20.1, "cmH2O", 1.0)
        );

        CalibrationRecord result = calibrationService.performCalibration(
                "DEV-TEST-01", "TECH-101", measurements, "Fluke VT900A", "Annual PM", testDate);

        assertEquals(CalibrationStatus.PASSED, result.getStatus());
        assertNotNull(result.getCertificate());
        assertTrue(result.getCertificate().getCertificateNumber().startsWith("CERT-"));

        // Verify device updated state
        LifeSupportDevice updated = (LifeSupportDevice) deviceRepository.findById("DEV-TEST-01").orElseThrow();
        assertEquals(DeviceStatus.OPERATIONAL, updated.getDeviceStatus());
        assertEquals(testDate, updated.getLastCalibrationDate());
        assertEquals(testDate.plusDays(90), updated.getNextCalibrationDate());
    }

    @Test
    @DisplayName("Failed calibration locks device OUT_OF_SERVICE without certificate")
    void testFailedCalibrationMarksDeviceOutOfService() {
        LifeSupportDevice defib = new LifeSupportDevice("DEV-TEST-02", "AST-DEFIB-99", "SN-99",
                "Defibrillator", "OEM", "D1", "ER", 90, 120, true);
        deviceRepository.save(defib);

        List<CalibrationMeasurement> measurements = List.of(
                new CalibrationMeasurement("Energy Discharge", 200.0, 180.0, "Joules", 5.0) // 20J deviation > 5J tol
        );

        CalibrationRecord result = calibrationService.performCalibration(
                "DEV-TEST-02", "TECH-101", measurements, "Impulse 7000DP", "Failed Test", LocalDate.now());

        assertEquals(CalibrationStatus.FAILED, result.getStatus());
        assertNull(result.getCertificate(), "Failed test must not generate certificate");

        LifeSupportDevice updated = (LifeSupportDevice) deviceRepository.findById("DEV-TEST-02").orElseThrow();
        assertEquals(DeviceStatus.OUT_OF_SERVICE, updated.getDeviceStatus());
        assertFalse(updated.canBeOperated());
    }

    @Test
    @DisplayName("Calibration due-status boundary checks (VALID, DUE_SOON, DUE_TODAY, OVERDUE, NOT_REQUIRED)")
    void testCalibrationDueStatusBoundaries() {
        LocalDate refDate = LocalDate.of(2026, 6, 15);

        LifeSupportDevice dev = new LifeSupportDevice("DEV-BOUND", "AST-BND", "SN-BND",
                "Test Device", "OEM", "M1", "ICU", 180, 120, true);

        // Not required check
        dev.setCalibrationRequired(false);
        assertEquals(CalibrationDueStatus.NOT_REQUIRED, dev.getCalibrationDueStatus(refDate));
        dev.setCalibrationRequired(true);

        // Never calibrated
        dev.setNextCalibrationDate(null);
        assertEquals(CalibrationDueStatus.OVERDUE, dev.getCalibrationDueStatus(refDate));

        // Overdue (yesterday)
        dev.setNextCalibrationDate(refDate.minusDays(1));
        assertEquals(CalibrationDueStatus.OVERDUE, dev.getCalibrationDueStatus(refDate));

        // Due today
        dev.setNextCalibrationDate(refDate);
        assertEquals(CalibrationDueStatus.DUE_TODAY, dev.getCalibrationDueStatus(refDate));

        // Due soon (within 7 days)
        dev.setNextCalibrationDate(refDate.plusDays(3));
        assertEquals(CalibrationDueStatus.DUE_SOON, dev.getCalibrationDueStatus(refDate));
        dev.setNextCalibrationDate(refDate.plusDays(7));
        assertEquals(CalibrationDueStatus.DUE_SOON, dev.getCalibrationDueStatus(refDate));

        // Valid (more than 7 days ahead)
        dev.setNextCalibrationDate(refDate.plusDays(8));
        assertEquals(CalibrationDueStatus.VALID, dev.getCalibrationDueStatus(refDate));
        dev.setNextCalibrationDate(refDate.plusDays(60));
        assertEquals(CalibrationDueStatus.VALID, dev.getCalibrationDueStatus(refDate));
    }
}
