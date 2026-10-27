package com.hospital.biomedical;

import com.hospital.biomedical.exception.InvalidStateTransitionException;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.enums.MaintenanceType;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.model.maintenance.PreventiveMaintenance;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.MaintenanceRepository;
import com.hospital.biomedical.repository.UserRepository;
import com.hospital.biomedical.service.AuditService;
import com.hospital.biomedical.service.AuthService;
import com.hospital.biomedical.service.MaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for Maintenance workflows, legal state transitions, and cost validations.
 */
public class MaintenanceTest {

    @TempDir
    Path tempDir;

    private DeviceRepository deviceRepository;
    private MaintenanceRepository maintenanceRepository;
    private MaintenanceService maintenanceService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        deviceRepository = new DeviceRepository(tempDir.resolve("devices.csv"));
        maintenanceRepository = new MaintenanceRepository(tempDir.resolve("records.csv"), tempDir.resolve("requests.csv"));
        UserRepository userRepository = new UserRepository(tempDir.resolve("users.csv"));
        AuditService auditService = new AuditService(new com.hospital.biomedical.repository.AuditRepository(tempDir.resolve("audit.csv")));
        authService = new AuthService(userRepository, auditService);
        maintenanceService = new MaintenanceService(maintenanceRepository, deviceRepository, authService, auditService);

        authService.login("admin", "Admin@123");
    }

    @Test
    @DisplayName("Negative maintenance cost and downtime are rejected")
    void testNegativeCostAndDowntimeRejection() {
        PreventiveMaintenance pm = new PreventiveMaintenance();

        assertThrows(IllegalArgumentException.class, () -> pm.setCost(new BigDecimal("-100.00")));
        assertThrows(IllegalArgumentException.class, () -> pm.setDowntimeHours(-5.0));

        pm.setCost(new BigDecimal("150.50"));
        pm.setDowntimeHours(3.0);
        assertEquals(new BigDecimal("150.50"), pm.getCost());
        assertEquals(3.0, pm.getDowntimeHours());
    }

    @Test
    @DisplayName("Invalid maintenance state transitions are prevented")
    void testInvalidStateTransitions() {
        LifeSupportDevice vent = new LifeSupportDevice("DEV-M1", "AST-M1", "SN-M1",
                "Ventilator", "OEM", "V1", "ICU", 180, 120, true);
        deviceRepository.save(vent);

        MaintenanceRequest req = maintenanceService.createRequest("DEV-M1", MaintenanceType.PREVENTIVE, "Filter check");
        assertEquals(MaintenanceStatus.REQUESTED, req.getStatus());

        // Cannot start directly if we enforce approval (or if request is already in progress)
        AbstractMaintenanceRecord record = maintenanceService.startMaintenance(req.getId(), "TECH-101");
        assertEquals(MaintenanceStatus.IN_PROGRESS, record.getMaintenanceStatus());

        // Attempting to start again should fail
        assertThrows(InvalidStateTransitionException.class, () ->
                maintenanceService.startMaintenance(req.getId(), "TECH-101"));

        // Complete the record
        maintenanceService.completeMaintenance(record.getId(), BigDecimal.valueOf(200), 2.0,
                List.of("Sensor"), List.of("Checked"), "All good");

        assertEquals(MaintenanceStatus.COMPLETED, record.getMaintenanceStatus());

        // Reopening completed maintenance must fail
        assertThrows(InvalidStateTransitionException.class, () ->
                maintenanceService.completeMaintenance(record.getId(), BigDecimal.valueOf(100), 1.0, null, null, null));
    }

    @Test
    @DisplayName("Device with failed calibration or replaced parts is NOT automatically made operational")
    void testSafetyRecalibrationEnforcedAfterMaintenance() {
        LifeSupportDevice vent = new LifeSupportDevice("DEV-SAFE", "AST-SAFE", "SN-SAFE",
                "Ventilator", "OEM", "V1", "ICU", 180, 120, true);
        vent.setDeviceStatus(DeviceStatus.OUT_OF_SERVICE); // Out of service due to fault
        deviceRepository.save(vent);

        MaintenanceRequest req = maintenanceService.createRequest("DEV-SAFE", MaintenanceType.CORRECTIVE, "Oxygen valve replaced");
        AbstractMaintenanceRecord record = maintenanceService.startMaintenance(req.getId(), "TECH-101");

        // Complete maintenance with replaced parts
        maintenanceService.completeMaintenance(record.getId(), BigDecimal.valueOf(500), 4.0,
                List.of("O2 Proportional Valve"), List.of("Leak test"), "Valve installed");

        // Confirm device status is CALIBRATION_PENDING, NOT OPERATIONAL!
        LifeSupportDevice updated = (LifeSupportDevice) deviceRepository.findById("DEV-SAFE").orElseThrow();
        assertEquals(DeviceStatus.CALIBRATION_PENDING, updated.getDeviceStatus());
        assertFalse(updated.canBeOperated(), "Device must not be clinically operated until certified calibrated.");
    }
}
