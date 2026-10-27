package com.hospital.biomedical;

import com.hospital.biomedical.exception.DuplicateEntityException;
import com.hospital.biomedical.exception.UnauthorizedAccessException;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.repository.AuditRepository;
import com.hospital.biomedical.repository.DeviceRepository;
import com.hospital.biomedical.repository.UserRepository;
import com.hospital.biomedical.service.AuditService;
import com.hospital.biomedical.service.AuthService;
import com.hospital.biomedical.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for Device registration, unique constraints, role authorization, and audit logs.
 */
public class AuthAndDeviceTest {

    @TempDir
    Path tempDir;

    private DeviceRepository deviceRepository;
    private AuditRepository auditRepository;
    private UserRepository userRepository;
    private AuditService auditService;
    private AuthService authService;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceRepository = new DeviceRepository(tempDir.resolve("dev.csv"));
        auditRepository = new AuditRepository(tempDir.resolve("aud.csv"));
        userRepository = new UserRepository(tempDir.resolve("usr.csv"));
        auditService = new AuditService(auditRepository);
        authService = new AuthService(userRepository, auditService);
        deviceService = new DeviceService(deviceRepository, authService, auditService);
    }

    @Test
    @DisplayName("Duplicate device ID and Asset Number are strictly rejected")
    void testDeviceIdAndAssetUniqueness() {
        authService.login("admin", "Admin@123");

        LifeSupportDevice d1 = new LifeSupportDevice("DEV-U1", "AST-DUP-1", "SN-1", "Vent 1", "OEM", "M1", "ICU", 180, 120, true);
        deviceService.registerDevice(d1);

        // Attempting to register another device with same ID
        LifeSupportDevice d2 = new LifeSupportDevice("DEV-U1", "AST-DUP-2", "SN-2", "Vent 2", "OEM", "M1", "ICU", 180, 120, true);
        assertThrows(DuplicateEntityException.class, () -> deviceService.registerDevice(d2));

        // Attempting to register another device with same Asset Number
        LifeSupportDevice d3 = new LifeSupportDevice("DEV-U2", "AST-DUP-1", "SN-3", "Vent 3", "OEM", "M1", "ICU", 180, 120, true);
        assertThrows(DuplicateEntityException.class, () -> deviceService.registerDevice(d3));
    }

    @Test
    @DisplayName("Role-based access restricts staff from registering or retiring equipment")
    void testRoleBasedPermissions() {
        // Login as staff
        authService.login("staff1", "Staff@123");

        LifeSupportDevice d = new LifeSupportDevice("DEV-SEC", "AST-SEC", "SN-SEC", "Vent", "OEM", "M1", "ICU", 180, 120, true);

        // Staff cannot register devices
        assertThrows(UnauthorizedAccessException.class, () -> deviceService.registerDevice(d));

        // Login as technician
        authService.login("tech1", "Tech@123");
        // Technician cannot retire devices
        assertThrows(UnauthorizedAccessException.class, () -> deviceService.retireDevice("DEV-SEC", "Old"));
    }

    @Test
    @DisplayName("Audit service logs events with username, action, and target")
    void testAuditLogging() {
        authService.login("admin", "Admin@123");

        LifeSupportDevice d = new LifeSupportDevice("DEV-AUD", "AST-AUD", "SN-AUD", "Ventilator", "OEM", "M1", "ICU", 180, 120, true);
        deviceService.registerDevice(d);

        deviceService.retireDevice(d.getId(), "Damaged in transit");

        assertEquals(2, auditRepository.findAll().stream()
                .filter(l -> l.getTargetId().equals(d.getId()))
                .count());
    }

    @Test
    @DisplayName("Retired device cannot be accidentally reactivated")
    void testRetiredDeviceSafetyLock() {
        authService.login("admin", "Admin@123");

        LifeSupportDevice d = new LifeSupportDevice("DEV-RET", "AST-RET", "SN-RET", "Vent", "OEM", "M1", "ICU", 180, 120, true);
        deviceService.registerDevice(d);
        deviceService.retireDevice(d.getId(), "End of life");

        LifeSupportDevice retrieved = (LifeSupportDevice) deviceRepository.findById("DEV-RET").orElseThrow();
        assertEquals(DeviceStatus.RETIRED, retrieved.getDeviceStatus());

        // Attempting to change status via setter
        assertThrows(IllegalStateException.class, () -> retrieved.setDeviceStatus(DeviceStatus.OPERATIONAL));

        // Attempting to calibrate retired device
        assertThrows(IllegalStateException.class, () -> retrieved.recordCalibration(java.time.LocalDate.now(), true));
    }
}
