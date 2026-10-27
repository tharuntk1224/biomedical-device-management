package com.hospital.biomedical;

import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.CalibrationStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.RiskLevel;
import com.hospital.biomedical.repository.CalibrationRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for File Persistence and Atomic Reload across application restarts.
 */
public class PersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Device and Calibration records survive restart and reload completely")
    void testPersistenceAndReload() {
        Path devPath = tempDir.resolve("devices.csv");
        Path calPath = tempDir.resolve("calibrations.csv");
        Path measPath = tempDir.resolve("meas.csv");

        // 1. Initial Session: write data
        DeviceRepository devRepo1 = new DeviceRepository(devPath);
        CalibrationRepository calRepo1 = new CalibrationRepository(calPath, measPath);

        LifeSupportDevice vent = new LifeSupportDevice("DEV-PERSIST-1", "AST-P1", "SN-P1",
                "Servo-u Ventilator", "Getinge", "Servo-u", "ICU", 180, 150, true);
        vent.setDeviceStatus(DeviceStatus.OPERATIONAL);
        vent.setLastCalibrationDate(LocalDate.of(2026, 3, 1));
        vent.setNextCalibrationDate(LocalDate.of(2026, 8, 28));
        devRepo1.save(vent);

        DiagnosticDevice ecg = new DiagnosticDevice("DEV-PERSIST-2", "AST-P2", "SN-P2",
                "Cardiostat 12", "Schiller", "AT-102", "Cardiology", RiskLevel.MEDIUM, 365, false, "v2.0");
        devRepo1.save(ecg);

        CalibrationRecord cal = new CalibrationRecord("CAL-P1", "DEV-PERSIST-1", "TECH-101",
                LocalDate.of(2026, 3, 1), "VT900A", "Test PM");
        cal.addMeasurement(new CalibrationMeasurement("PEEP", 10.0, 10.1, "cmH2O", 0.5));
        cal.evaluate();
        calRepo1.save(cal);

        assertEquals(2, devRepo1.count());
        assertEquals(1, calRepo1.count());

        // 2. Simulated Restart: initialize brand new repository instances from disk
        DeviceRepository devRepo2 = new DeviceRepository(devPath);
        CalibrationRepository calRepo2 = new CalibrationRepository(calPath, measPath);

        assertEquals(2, devRepo2.count());
        assertEquals(1, calRepo2.count());

        LifeSupportDevice reloadedVent = (LifeSupportDevice) devRepo2.findById("DEV-PERSIST-1").orElseThrow();
        assertEquals("Servo-u Ventilator", reloadedVent.getName());
        assertEquals(150, reloadedVent.getBatteryBackupMinutes());
        assertTrue(reloadedVent.isRedundantAlarmsOperational());
        assertEquals(DeviceStatus.OPERATIONAL, reloadedVent.getDeviceStatus());
        assertEquals(LocalDate.of(2026, 3, 1), reloadedVent.getLastCalibrationDate());

        CalibrationRecord reloadedCal = calRepo2.findById("CAL-P1").orElseThrow();
        assertEquals("DEV-PERSIST-1", reloadedCal.getDeviceId());
        assertEquals(CalibrationStatus.PASSED, reloadedCal.getStatus());
        assertEquals(1, reloadedCal.getMeasurements().size());
        assertEquals("PEEP", reloadedCal.getMeasurements().get(0).getParameterName());
    }

    @Test
    @DisplayName("Missing data files are handled gracefully without exceptions")
    void testMissingFilesHandledGracefully() {
        Path missingPath = tempDir.resolve("non_existent_folder").resolve("devices.csv");
        assertDoesNotThrow(() -> {
            DeviceRepository repo = new DeviceRepository(missingPath);
            assertEquals(0, repo.count());
        });
    }
}
